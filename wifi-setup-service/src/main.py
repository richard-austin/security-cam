#! /usr/bin/python3
from datetime import datetime
import json
from types import SimpleNamespace
import os
import re
import logging
from logging.handlers import TimedRotatingFileHandler

from pytz import timezone
from cgi import parse_header
from http.server import BaseHTTPRequestHandler, HTTPServer
from wifi_details import WifiDetails
from connection_state_details import ConnectionStateDetails

tz = timezone('Europe/London')  # UTC, Asia/Shanghai, Europe/Berlin


def timetz(*args):
    return datetime.now(tz).timetuple()


FORMAT = '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
logging.basicConfig(encoding='utf-8', level=logging.INFO, format=FORMAT)
logging.Formatter.converter = timetz
logger = logging.getLogger('wifimgr')

# Set up a handler for time rotated file logging
logHandler = TimedRotatingFileHandler(filename="/var/log/wifimgr/wifimgr.log",
                                      when="midnight", interval=1, backupCount=30)
logHandler.setFormatter(logging.Formatter(FORMAT))
# add ch to logger
logger.addHandler(logHandler)


def get_devices() -> list[SimpleNamespace]:
    data = executeOsCommand('nmcli -t dev show', Handler.nmcli_errors)
    devices_str: list[str] = data.split("\n\n")
    devices: list[SimpleNamespace] = []
    for i in range(len(devices_str)):
        lines = devices_str[i].split('\n')
        sn: SimpleNamespace = SimpleNamespace()
        for line in lines:
            attrval = line.split(':', 1)
            if len(attrval) == 2:  # Skip the blank line at the end
                # Replace the dot separator in the attribute name with  underscore
                attrval[0] = attrval[0].replace('.', '_')
                # Replace any dash separator with underscore
                attrval[0] = attrval[0].replace('-', '_')
                # Replace '[' with underscores and ']' with blank
                attrval[0] = attrval[0].replace('[', '_').replace(']', '')
                setattr(sn, attrval[0], attrval[1])
        devices.append(sn)
    return devices


def get_active_connections() -> list:
    data = get_devices()

    filt = filter(lambda conn:
                  (conn.GENERAL_TYPE == 'ethernet' and conn.WIRED_PROPERTIES_CARRIER.lower() == 'on')
                  or
                  (conn.GENERAL_TYPE == 'wifi' and conn.GENERAL_STATE.startswith('100')),
                  data)

    connections: list[SimpleNamespace] = list(filt)

    return connections


"""
    check_for_ethernet: Checks that we are connected through ethernet
    @:returns: true if connected via ethernet
"""


def check_for_ethernet() -> bool:
    retval: bool = False
    objs = get_active_connections()
    for obj in objs:
        if "GENERAL_TYPE" in obj.__dict__ and obj.GENERAL_TYPE == 'ethernet':
            retval = True
            break

    return retval


"""
    checkConnectionState: Checks that the wifi connection with the given ssid is active
    @params:
    @ssid: The wifi ssid to check
    @:returns: True if ssid is active else False
"""


def checkConnectionState(ssid: str):  # This works on a fresh install headless system
    retval: bool = False
    try:
        message = executeOsCommand(f"nmcli -t -f GENERAL.STATE con show {ssid}",
                                   Handler.nmcli_errors)
        result: []
        lines = message.split("\n")
        for line in lines:
            result = line.split(":")
            if len(result) == 2:
                connection_state = ConnectionStateDetails(general_state=result[1])
                if connection_state.general_state == "activated":
                    retval = True
                break
    except Exception as ex:
        logger.error(f"{ex}")

    return retval


def executeOsCommand(command: str, exception_messages: {int, str}) -> str:
    stream = os.popen(command)
    message = stream.read()
    exitcode = stream.close()
    if exitcode is not None:
        exitcode = os.WEXITSTATUS(exitcode)
        if exitcode in exception_messages:
            error_message = exception_messages[exitcode]
            raise Exception(f"{error_message}")
        else:
            raise Exception(f"Unknown error code {exitcode}")
    return message


class Handler(BaseHTTPRequestHandler):
    nmcli_errors = {
        0: "Success – indicates the operation succeeded.",
        1: "Unknown or unspecified error.",
        2: "Invalid user input, wrong nmcli invocation.",
        3: "Timeout expired (see --wait option).",
        4: "Connection activation failed.",
        5: "Connection deactivation failed.",
        6: "Disconnecting device failed.",
        7: "Connection deletion failed.",
        8: "NetworkManager is not running.",
        10: "Connection, device, or access point does not exist.",
    }

    systemd_errors = {
        0: "Success",
        200: "Changing to the requested working directory failed. See WorkingDirectory.",
        201: "Failed to set up process scheduling priority (nice level). See Nice.",
        202: "Failed to close unwanted file descriptors, or to adjust passed file descriptors.",
        203: "The actual process execution failed (specifically, the execve(2) system call). Most likely this is caused by a missing or non-accessible executable file.",
        204: "Failed to perform an action due to memory shortage.",
        205: "Failed to adjust resource limits. See LimitCPU= and related settings above.",
        206: "Failed to adjust the OOM setting. See OOMScoreAdjust.",
        207: "Failed to set process signal mask.",
        208: "Failed to set up standard input. See StandardInput.",
        209: "Failed to set up standard output. See StandardOutput.",
        210: "Failed to change root directory (chroot(2)). See RootDirectory=/RootImage.",
        211: "Failed to set up IO scheduling priority. See IOSchedulingClass=/IOSchedulingPriority.",
        212: "Failed to set up timer slack. See TimerSlackNSec.",
        213: "Failed to set process secure bits. See SecureBits.",
        214: "Failed to set up CPU scheduling. See CPUSchedulingPolicy=/CPUSchedulingPriority.",
        215: "Failed to set up CPU affinity. See CPUAffinity.",
        216: "Failed to determine or change group credentials. See Group=/SupplementaryGroups.",
        217: "Failed to determine or change user credentials, or to set up user namespacing. See User=/PrivateUsers.",
        218: "Failed to drop capabilities, or apply ambient capabilities. See CapabilityBoundingSet=/AmbientCapabilities.",
        219: "Setting up the service control group failed.",
        220: "Failed to create new process session.",
        221: "Execution has been cancelled by the user. See the systemd.confirm_spawn= kernel command line setting on kernel-command-line(7) for details.",
        222: "Failed to set up standard error output. See StandardError.",
        224: "Failed to set up PAM session. See PAMName.",
        225: "Failed to set up network namespacing. See PrivateNetwork.",
        226: "Failed to set up mount, UTS, or IPC namespacing. See ReadOnlyPaths=, ProtectHostname=, PrivateIPC=, and related settings above.",
        227: "Failed to disable new privileges. See NoNewPrivileges=yes above.",
        228: "Failed to apply system call filters. See SystemCallFilter= and related settings above.",
        229: "Determining or changing SELinux context failed. See SELinuxContext.",
        230: "Failed to set up an execution domain (personality). See Personality.",
        231: "Failed to prepare changing AppArmor profile. See AppArmorProfile.",
        232: "Failed to restrict address families. See RestrictAddressFamilies.",
        233: "Setting up runtime directory failed. See RuntimeDirectory= and related settings above.",
        235: "Failed to adjust socket ownership. Used for socket units only.",
        236: "Failed to set SMACK label. See SmackProcessLabel.",
        237: "Failed to set up kernel keyring.",
        238: "Failed to set up unit's state directory. See StateDirectory.",
        239: "Failed to set up unit's cache directory. See CacheDirectory.",
        240: "Failed to set up unit's logging directory. See LogsDirectory.",
        241: "Failed to set up unit's configuration directory. See ConfigurationDirectory.",
        242: "Failed to set up unit's NUMA memory policy. See NUMAPolicy= and NUMAMask.",
        243: "Failed to set up unit's credentials. See LoadCredential= and SetCredential.",
        245: "Failed to apply BPF restrictions. See RestrictFileSystems."
    }

    def returnResponse(self, http_status: int, response):
        if http_status == 200:
            logger.info(f"Return response {http_status}: {response}")
        else:
            logger.error(f"Return response: {http_status}: {response}")
        self.send_response(http_status)
        if type(response) is str:
            self.send_header('Content-type', 'text/html')
            self.end_headers()
            self.wfile.write(bytes(response, "utf8"))
        elif type(response) is list:
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(bytes(json.dumps(response, default=lambda ob: ob.__dict__), "utf8"))
        elif type(response) is dict:
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(bytes(json.dumps(response), "utf8"))
        else:
            raise Exception("Unknown response type")

        return

    def do_GET(self):
        self.returnResponse(400, "GET calls are not supported")

    def do_POST(self):
        try:
            cmd = self.parse_POST()

            match cmd['command']:
                case 'scanwifi':
                    logger.info("Scan for wifi access points")
                    try:
                        message = executeOsCommand('nmcli -t dev wifi', self.nmcli_errors)
                        wifis: [] = []
                        result: []
                        lines = message.split("\n")
                        for line in lines:
                            result = re.split(r"(?<!\\):", line)
                            if len(result) == 9:
                                wifi_details = WifiDetails(in_use=result[0] == '*',
                                                           bssid=result[1].replace('\\', ''),
                                                           ssid=result[2],
                                                           mode=result[3],
                                                           channel=result[4],
                                                           rate=result[5],
                                                           signal=result[6],
                                                           security=result[8])
                                wifis.append(wifi_details)
                        self.returnResponse(200, wifis)
                    except Exception as ex:
                        logger.error(f"Exception when trying to scan Wifi: {ex}")
                        self.returnResponse(500, f"An error occurred: {ex}")
                        return

                case 'setupwifi':
                    ssid: str = cmd['ssid']
                    logger.info(f"Setting up wifi for SSID {ssid}")
                    password: str = cmd['password'] if cmd.__contains__('password') else None
                    message: str
                    try:
                        if password is None:
                            message = executeOsCommand(f"nmcli dev wifi connect {ssid}",
                                                       self.nmcli_errors)
                        else:
                            message = executeOsCommand(f"nmcli dev wifi connect {ssid} password {password}",
                                                       self.nmcli_errors)
                    except Exception as ex:
                        logger.error(f"Exception when trying to set Wifi: {ex}")
                        self.returnResponse(500, f"An error occurred: {ex}")
                        return

                    activated = checkConnectionState(cmd['ssid'])
                    if activated:
                        self.returnResponse(200, f"Wifi connection to {cmd['ssid']} is active")
                    else:
                        self.returnResponse(400, f"Failed to activate Wifi connection to {cmd['ssid']}. {message}")
                    return

                case 'checkwifistatus':
                    logger.info("Check if wifi enabled")
                    message = executeOsCommand("nmcli radio wifi", self.nmcli_errors)
                    status = 'on' if message.strip('\n') == 'enabled' else 'off'
                    self.returnResponse(200, f'{{"status": "{status}"}}')
                    return

                case 'setwifistatus':
                    status = cmd["status"]
                    if {"on": 1, "off": 1}.__contains__(status):
                        logger.info(f"Set wifi {status}")
                        executeOsCommand(f"nmcli radio wifi {status}", self.nmcli_errors)
                        self.returnResponse(200, f'{{"status": "{status}"}}')
                    else:
                        self.returnResponse(400, f"Unknown status {status}")
                    return

                case 'getactiveconnections':
                    logger.info("Get active network connections")
                    conns: [object] = get_active_connections()
                    self.returnResponse(200, conns)
                    return

                case 'check_for_ethernet':
                    logger.info("Check if an ethernet connection is active")
                    ethernet: bool = check_for_ethernet()
                    self.returnResponse(200, {"ethernet": ethernet})
                    return
                # Options to start and stop the motion service. This requires root privileges
                # hence using this module which is otherwise meant for Wi-Fi and network related control

                case 'start_services':
                    executeOsCommand('systemctl start fmp4-ws-media-server.service', self.systemd_errors)
                    executeOsCommand('systemctl start motion.service', self.systemd_errors)
                    executeOsCommand('systemctl start camera-recordings.service', self.systemd_errors)
                    self.returnResponse(200, {"result": "Motion service started"})
                    return

                case 'stop_services':
                    executeOsCommand('systemctl stop motion.service', self.systemd_errors)
                    executeOsCommand('systemctl stop camera-recordings.service', self.systemd_errors)
                    executeOsCommand('systemctl stop fmp4-ws-media-server.service', self.systemd_errors)
                    self.returnResponse(200, {"result": "Motion service stopped"})
                    return

                case _:
                    self.returnResponse(400, f"Unknown command {cmd['command']}")
                    return

        except Exception as ex:
            self.returnResponse(500, ex.__str__())
            return

    def parse_POST(self):
        ctype, pdict = parse_header(self.headers['content-type'])
        if ctype == 'application/json':
            length = int(self.headers['content-length'])
            json_params = self.rfile.read(length)
            postvars = json.loads(json_params)
        else:
            postvars = {}
        return postvars


with HTTPServer(('localhost', 8000), Handler) as server:
    server.serve_forever()
