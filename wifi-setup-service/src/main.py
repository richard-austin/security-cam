#! /usr/bin/python3
import json
import os
import re
from cgi import parse_header
from http.server import BaseHTTPRequestHandler, HTTPServer
from wifi_details import WifiDetails
from connection_details import ConnectionDetails
from connection_state_details import ConnectionStateDetails


def obj_dict(obj):
    return obj.__dict__


"""
    check_for_ethernet: Checks that we are connected through ethernet
    @:returns: true if connected via ethernet
"""


def check_for_ethernet() -> bool:
    retval: bool = False
    stream = os.popen('nmcli -t con show')
    message = stream.read()
    result: []
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
    stream = os.popen(f"nmcli -t -f GENERAL.STATE con show {ssid}")
    message = stream.read()
    result: []
    lines = message.split("\n")
    for line in lines:
        result = line.split(":")
        if len(result) == 2:
            connection_state = ConnectionStateDetails(general_state=result[1])
            if connection_state.general_state == "activated":
                retval = True
                break

    return retval


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(400)
        self.send_header('Content-type', 'text/html')
        self.end_headers()
        message = "GET calls are not supported"
        self.wfile.write(bytes(message, "utf8"))

    def do_POST(self):
        try:
            cmd = self.parse_POST()

            match cmd['command']:
                case 'scanwifi':
                    self.send_response(200)
                    self.send_header('Content-type', 'text/html')
                    self.end_headers()

                    stream = os.popen('nmcli -t dev wifi')
                    message = stream.read()
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
                    self.wfile.write(bytes(json_str, "utf8"))

                case 'setupwifi':
                    hasethernet: bool = check_for_ethernet()
                    if not hasethernet:
                        self.send_response(401)
                        self.send_header('Content-type', 'text/html')
                        self.end_headers()
                        self.wfile.write(bytes("You must connect the NVR through Ethernet to change wifi settings",
                                               "utf8"))

                    self.send_response(200)
                    ssid: str = cmd['ssid']
                    password: str = cmd['password']
                    os.system(f"nmcli dev wifi connect {ssid} password {password}")
                    activated = checkConnectionState(cmd['ssid'])
                    if activated:
                        self.send_response(200)
                        self.send_header('Content-type', 'text/html')
                        self.end_headers()
                        self.wfile.write(bytes(f"Wifi connection to {cmd['ssid']} is active",
                                               "utf8"))
                    else:
                        self.send_response(500)
                        self.send_header('Content-type', 'text/html')
                        self.end_headers()
                        self.wfile.write(bytes(f"Failed to activate Wifi connection to {cmd['ssid']}",
                                               "utf8"))
                case _:
                    self.send_response(400)

        except Exception as ex:
            self.send_response(500)
            self.send_header('Content-type', 'text/html')
            self.end_headers()
            self.wfile.write(bytes(ex.__str__(), "utf8"))

    def parse_POST(self):
        ctype, pdict = parse_header(self.headers['content-type'])
        if ctype == 'application/json':
            length = int(self.headers['content-length'])
            json_params = self.rfile.read(length)
            postvars = json.loads(json_params)
        else:
            postvars = {}
        return postvars


with HTTPServer(('', 8000), Handler) as server:
    server.serve_forever()
