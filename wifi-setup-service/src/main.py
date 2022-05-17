#! /usr/bin/python3
from datetime import datetime
import json
import os
import re
import logging
from logging.handlers import TimedRotatingFileHandler

from pytz import timezone
from cgi import parse_header
from http.server import BaseHTTPRequestHandler, HTTPServer
from wifi_details import WifiDetails
from connection_details import ConnectionDetails
from connection_state_details import ConnectionStateDetails


def obj_dict(obj):
    return obj.__dict__


tz = timezone('Europe/London')  # UTC, Asia/Shanghai, Europe/Berlin


def timetz(*args):
    return datetime.now(tz).timetuple()


FORMAT = '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
logging.basicConfig(encoding='utf-8', level=logging.INFO, format=FORMAT)
logging.Formatter.converter = timetz
logger = logging.getLogger('wifimgr')

# Set up a handler for time rotated file logging
logHandler = TimedRotatingFileHandler(filename="/var/log/wifimgr/wifimgr.log", when="midnight")
logHandler.setFormatter(logging.Formatter(FORMAT))
# add ch to logger
logger.addHandler(logHandler)

"""
    check_for_ethernet: Checks that we are connected through ethernet
    @:returns: true if connected via ethernet
"""


def check_for_ethernet() -> bool:
    retval: bool = False
    message = executeOsCommand('nmcli -t con show', Handler.nmcli_errors)
    lines = message.split("\n")
    for line in lines:
        result = line.split(":")
        if len(result) == 4:
            connection_details = ConnectionDetails(name=result[0],
                                                   uuid=result[1].replace('\\', ''),
                                                   c_type=result[2],
                                                   device=result[3])
            if connection_details.con_type.__contains__("ethernet") and connection_details.device != "":
                retval = True
                break

    return retval


"""
    checkConnectionState: Checks that the wifi connection with the given ssid is active
    @params:
    @ssid: The wifi ssid to check
    @:returns: True if ssid is active else False
"""


def checkConnectionState(ssid: str):
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
        error_message = exception_messages[exitcode]
        if error_message is not None:
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
        10: "Connection, device, or access point does not exist."
    }

    def returnResponse(self, http_status: int, response: str):
        if http_status == 200:
            logger.info(f"Return response {http_status}: {response}")
        else:
            logger.error(f"Return response: {http_status}: {response}")
        self.send_response(http_status)
        self.send_header('Content-type', 'text/html')
        self.end_headers()
        self.wfile.write(bytes(response, "utf8"))
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

                        json_str = json.dumps(wifis, default=obj_dict)
                        self.returnResponse(200, json_str)
                    except Exception as ex:
                        logger.error(f"Exception when trying to scan Wifi: {ex}")
                        self.returnResponse(500, f"An error occurred: {ex}")
                        return

                case 'setupwifi':
                    hasethernet: bool = check_for_ethernet()
                    if not hasethernet:
                        self.returnResponse(401, "You must connect the NVR through Ethernet to change wifi settings")
                        return

                    ssid: str = cmd['ssid']
                    logger.info(f"Setting up wifi for SSID {ssid}")
                    password: str = cmd['password'] if cmd.__contains__('password') else None
                    message: str
                    try:
                        if password is None:
                            executeOsCommand(f"nmcli dev wifi connect {ssid}",
                                             self.nmcli_errors)
                        else:
                            executeOsCommand(f"nmcli dev wifi connect {ssid} password {password}",
                                             self.nmcli_errors)
                    except Exception as ex:
                        logger.error(f"Exception when trying to set Wifi: {ex}")
                        self.returnResponse(500, f"An error occurred: {ex}")
                        return

                    activated = checkConnectionState(cmd['ssid'])
                    if activated:
                        self.returnResponse(200, f"Wifi connection to {cmd['ssid']} is active")
                    else:
                        self.returnResponse(400, f"Failed to activate Wifi connection to {cmd['ssid']}. Please check "
                                                 "the SSID and password are correct")
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
