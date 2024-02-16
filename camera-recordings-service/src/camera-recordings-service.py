import json
import logging
import os
import signal
import subprocess
import threading
import time
from datetime import datetime
from email.message import EmailMessage
from enum import Enum
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from logging.handlers import TimedRotatingFileHandler

from pyftpdlib.handlers import FTPHandler
from pyftpdlib.servers import ThreadedFTPServer
from pyftpdlib.authorizers import DummyAuthorizer
from pytz import timezone
from resettabletimer import ResettableTimer

tz = timezone('Europe/London')  # UTC, Asia/Shanghai, Europe/Berlin


def timetz(*args):
    return datetime.now(tz).timetuple()


FORMAT = '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
logging.basicConfig(encoding='utf-8', level=logging.INFO, format=FORMAT)
logging.Formatter.converter = timetz
logger = logging.getLogger('rec-svc')

# Set up a handler for time rotated file logging
logHandler = TimedRotatingFileHandler(filename="/var/log/camera-recordings-service/recordings.log",
                                      when="midnight", interval=1, backupCount=30)
logHandler.setFormatter(logging.Formatter(FORMAT))
# add ch to logger
logger.addHandler(logHandler)


class CameraType(Enum):
    none = 0
    sv3c = 1
    zxtechMCW5B10X = 2


def execute_os_command(command: str) -> [int, str]:
    stream = os.popen(command)
    message: str = stream.read()
    exitcode = stream.close()
    return {exitcode, message}


def get_ffmpeg_cmd(camera: any):
    if camera is not None and camera["ftp"]:
        first_stream = next(iter(camera['streams']))
    location = camera['streams'][first_stream]['recording']['location']
    audio = camera["streams"][first_stream]["audio"]
    recording_src_url = camera['streams'][first_stream]['recording']['recording_src_url']
    epoch_time = int(time.time())
    ffmpeg_cmd: str = (
        f"/usr/local/bin/ffmpeg -i {recording_src_url} -t 01:00:00 {'-c:a copy' if audio else '-an'}"
        f" -c:v copy -level 3.0 -start_number 0 -hls_time 3 -hls_list_size 0 -hls_segment_type"
        f" fmp4 -hls_fmp4_init_filename {location}-{epoch_time}_.mp4"
        f" -f hls /var/security-cam/{location}/{location}-{epoch_time}_.m3u8")
    return ffmpeg_cmd


class FTPAndVideoFileProcessor(FTPHandler):
    recordingsPath: str = "/var/security-cam"
    ftpPath: str = f"{recordingsPath}/ftp"

    processDict: dict = dict()

    def on_connect(self):
        logger.info(f"{self.remote_ip}:{self.remote_port} connected")

    def on_disconnect(self):
        # do something when client disconnects
        pass

    def on_login(self, username):
        # do something when user login
        pass

    def on_logout(self, username):
        # do something when user logs out
        pass

    def on_file_sent(self, file):
        # do something when a file has been sent
        logger.info(f"File sent: {self!r} -- {file}")

    def on_file_received(self, file):
        # do something when a file has been received
        logger.info(f"File received: {file}")
        self.trigger_recording_from_ftp_file(file)

    def on_incomplete_file_sent(self, file):
        # do something when a file is partially sent
        pass

    def on_incomplete_file_received(self, file):
        # remove partially uploaded files
        import os
        os.remove(file)

    def create_recording_path(self, ftp_file_path: str):
        recording_path: str = ftp_file_path.replace(self.ftpPath, '')

    def finish_recording(self, subproc: subprocess, location: str):
        subproc.send_signal(signal.SIGINT)
        subproc.wait()
        logger.info(f"Recording ended for {location}")
        self.processDict.pop(location)

    def trigger_recording_from_ftp_file(self, path: str):
        cams_file = open("/var/security-cam/cameras.json")
        location: str = ""
        try:
            cams = json.load(cams_file)
            camera_name: str = path.replace(self.ftpPath, '', 1)
            camera_name = camera_name[1: camera_name.index('/', 1)]
            camera = cams[camera_name]
            cam_type: CameraType = camera['cameraParamSpecs']['camType']
            match cam_type:
                case CameraType.sv3c.value | CameraType.zxtechMCW5B10X.value | CameraType.none.value:
                    logger.info(f"Camera type {cam_type}")
                case _:
                    logger.warning(f"No camera type for file {path}")

            if path.endswith('.jpg'):  # Only dealing with jpg files
                if self.processDict.__contains__(location):
                    self.processDict[location].reset()
                else:
                    ffmpeg_cmd = get_ffmpeg_cmd(camera)
                    subproc: subprocess.Popen = subprocess.Popen(ffmpeg_cmd.split(), stdout=subprocess.PIPE)
                    timer: ResettableTimer = ResettableTimer(camera['retriggerWindow'],
                                                             lambda: self.finish_recording(subproc, location))
                    timer.start()
                    self.processDict[location] = timer
                    logger.info(
                        f"Started recording for {location}, retriggerWindow = {camera['retriggerWindow']}")

                os.remove(path)

                # Remove old directories and any remaining files created by camera FTP transfers
                execute_os_command(f"find {self.ftpPath} -mtime +2 -delete")
                # Remove recording files more than 3 weeks old
                if location != "":
                    execute_os_command(f"find {self.recordingsPath}/{location} -mtime +21 -delete")

        except TypeError as t:
            logger.error(f"Exception TypeError was raised {t!r}")

        except KeyError as k:
            logger.error(f"Exception KeyError was raised {k!r}")

        except Exception as e:
            logger.error(f"An exception occurred: {e!r}")


class HttpHandler(BaseHTTPRequestHandler):
    recording_procs: dict = {}

    def do_POST(self):
        try:
            cams_file = open("/var/security-cam/cameras.json")
            cams = json.load(cams_file)

            cmd = self.parse_POST()
            match cmd['command']:
                case 'start_recording':
                    camera_name = cmd['camera_name']
                    logger.info(f"Starting recording for {camera_name}")
                    camera = cams[camera_name]
                    cam_type: CameraType = camera['cameraParamSpecs']['camType']
                    ffmpeg_cmd = get_ffmpeg_cmd(camera)
                    # Check there is not already an entry for this camera before starting recording
                    if not self.recording_procs.__contains__(camera_name):
                        subproc: subprocess.Popen = subprocess.Popen(ffmpeg_cmd.split(), stdout=subprocess.PIPE)
                        self.recording_procs[camera_name] = subproc
                        self.returnResponse(200, f"Recording started for {camera_name}")
                    else:
                        self.returnResponse(400, f"Recording already underway for {camera_name}")

                case 'end_recording':
                    camera_name = cmd['camera_name']
                    # Check there is a recording process for this camera
                    if self.recording_procs.__contains__(camera_name):
                        logger.info("Stopping recording")
                        subproc = self.recording_procs[camera_name]
                        subproc.send_signal(signal.SIGINT)
                        subproc.wait()
                        logger.info(f"Recording ended for {camera_name}")
                        self.recording_procs.pop(camera_name)
                    else:
                        self.returnResponse(400, f"No recording process exists for {camera_name}")


        except Exception as ex:
            self.returnResponse(500, ex.__str__())
            return

    def do_GET(self):
        self.returnResponse(400, "GET calls are not supported")

    def parse_POST(self):
        msg = EmailMessage()
        msg['content-type'] = self.headers['content-type']
        ctype = msg.get_content_type()

        if ctype == 'application/json':
            length = int(self.headers['content-length'])
            json_params = self.rfile.read(length)
            postvars = json.loads(json_params)
        else:
            postvars = {}
        return postvars

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


with ThreadingHTTPServer(('localhost', 8086), HttpHandler) as http_server:
    server_thread = threading.Thread(target=http_server.serve_forever)
    server_thread.daemon = True
    server_thread.start()

    authorizer = DummyAuthorizer()
    authorizer.add_user('user', '12345', homedir=FTPAndVideoFileProcessor.ftpPath, perm='elradfmwMT')
    authorizer.add_anonymous(homedir='.')

    handler = FTPAndVideoFileProcessor
    handler.authorizer = authorizer
    ftp_server = ThreadedFTPServer(('', 2121), handler)
    ftp_server.serve_forever()

    http_server.shutdown()
    server_thread.join()
