import json
import logging
import os
import signal
import subprocess
import time
from datetime import datetime
from enum import Enum
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


def executeOsCommand(command: str) -> [int, str]:
    stream = os.popen(command)
    message: str = stream.read()
    exitcode = stream.close()
    return {exitcode, message}


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
        self.triggerRecordingFromFTPFile(file)

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

    def triggerRecordingFromFTPFile(self, path: str):
        cams_file = open("/var/security-cam/cameras.json")
        location: str = ""

        try:
            cams = json.load(cams_file)
            camera_name: str = path.replace(self.ftpPath, '', 1)
            camera_name = camera_name[1: camera_name.index('/', 1)]
            camera = cams[camera_name]
            ffmpeg_cmd: str = ""
            if camera is not None and camera["ftp"]:
                if path.endswith('.jpg'):  # Only dealing with jpg files
                    cam_type: CameraType = camera['cameraParamSpecs']['camType']

                    first_stream = next(iter(camera['streams']))
                    location = camera['streams'][first_stream]['recording']['location']
                    audio = camera["streams"][first_stream]["audio"]
                    recording_src_url = camera['streams'][first_stream]['recording']['recording_src_url']
                    epoch_time = int(time.time())
                    match cam_type:
                        case CameraType.sv3c.value | CameraType.zxtechMCW5B10X.value | CameraType.none.value:
                            ffmpeg_cmd = (
                                f"/usr/local/bin/ffmpeg -i {recording_src_url} -t 01:00:00 {'-c:a copy' if audio else '-an'}"
                                f" -c:v copy -level 3.0 -start_number 0 -hls_time 3 -hls_list_size 0 -hls_segment_type"
                                f" fmp4 -hls_fmp4_init_filename {location}-{epoch_time}_.mp4"
                                f" -f hls /var/security-cam/{location}/{location}-{epoch_time}_.m3u8")
                        case _:
                            logger.warning(f"No camera type for file {path}")

                    if ffmpeg_cmd != "":
                        if self.processDict.__contains__(location):
                            self.processDict[location].reset()
                        else:
                            subproc: subprocess.Popen = subprocess.Popen(ffmpeg_cmd.split(), stdout=subprocess.PIPE)
                            timer: ResettableTimer = ResettableTimer(30, lambda: self.finish_recording(subproc, location))
                            timer.start()
                            self.processDict[location] = timer
                            logger.info(f"Started recording for {location}")

            os.remove(path)

            # Remove old directories and any remaining files created by camera FTP transfers
            executeOsCommand(f"find {self.ftpPath} -mtime +2 -delete")
            # Remove recording files more than 3 weeks old
            if location != "":
                executeOsCommand(f"find {self.recordingsPath}/{location} -mtime +21 -delete")

        except TypeError as t:
            logger.error(f"Exception TypeError was raised {t!r}")
        except KeyError as k:
            logger.error(f"Exception KeyError was raised {k!r}")
        except Exception as e:
            logger.error(f"An exception occurred: {e!r}")


def main():
    # bashCmd: str = "ffmpeg -i rtsp://192.168.0.23:554/11 -t 01:00:00 -an -c copy -level 3.0 -start_number 0 -hls_time 3 -hls_list_size 0 -f hls /var/security-cam/rec1/rec1_.m3u8"
    # process = subprocess.Popen(bashCmd.split(), stdout=subprocess.PIPE)
    # pid: int = process.pid
    # p = process
    authorizer = DummyAuthorizer()
    authorizer.add_user('user', '12345', homedir=FTPAndVideoFileProcessor.ftpPath, perm='elradfmwMT')
    authorizer.add_anonymous(homedir='.')

    handler = FTPAndVideoFileProcessor
    handler.authorizer = authorizer
    server = ThreadedFTPServer(('', 2121), handler)
    server.serve_forever()


if __name__ == "__main__":
    main()
