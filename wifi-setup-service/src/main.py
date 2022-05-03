#! /usr/bin/python3
import json
import os
import re
from cgi import parse_header
from http.server import BaseHTTPRequestHandler, HTTPServer
from wifi_details import WifiDetails


def obj_dict(obj):
    return obj.__dict__


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.send_header('Content-type', 'text/html')
        self.end_headers()

        message = "Hello, World! Here is a GET response"
        self.wfile.write(bytes(message, "utf8"))

    def do_POST(self):
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
                self.send_response(200)

            case _:
                self.send_response(400)

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
