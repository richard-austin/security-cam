import json
import logging
import os
from datetime import datetime
from enum import Enum
from logging.handlers import TimedRotatingFileHandler

from pyftpdlib.handlers import FTPHandler
from pyftpdlib.servers import ThreadedFTPServer
from pyftpdlib.authorizers import DummyAuthorizer
from pytz import timezone

tz = timezone('Europe/London')  # UTC, Asia/Shanghai, Europe/Berlin


def timetz(*args):
    return datetime.now(tz).timetuple()


FORMAT = '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
logging.basicConfig(encoding='utf-8', level=logging.INFO, format=FORMAT)
logging.Formatter.converter = timetz
logger = logging.getLogger('rec-svc')

# Set up a handler for time rotated file logging
logHandler = TimedRotatingFileHandler(filename="/var/log/camera-recordings-service/recordings.log", when="midnight")
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
        self.convert264File(file)

    def on_incomplete_file_sent(self, file):
        # do something when a file is partially sent
        pass

    def on_incomplete_file_received(self, file):
        # remove partially uploaded files
        import os
        os.remove(file)

    def create_recording_path(self, ftp_file_path: str):
        recording_path: str = ftp_file_path.replace(self.ftpPath, '')

    """
        convert264File: Convert an .264 file received from a camera to an hls file
    """

    def convert264File(self, path: str):
        f = open("/etc/security-cam/cameras.json")
        try:
            cams = json.load(f)

            # recording_path = path.replace(self.ftpPath, '')
            camera_name: str = path.replace(self.ftpPath, '', 1)
            camera_name = camera_name[1: camera_name.index('/', 1)]
            camera = cams[camera_name]
            ffmpeg_cmd: str = ""
            if camera is not None:
                cam_type: CameraType = camera['cameraParamSpecs']['camType']
                first_stream = next(iter(camera['streams']))
                location: str = camera['streams'][first_stream]['recording']['location']
                match cam_type:
                    case CameraType.sv3c.value:
                        ffmpeg_cmd = (f"ffmpeg -i {path} -t 01:00:00 -an -bsf:v h264_mp4toannexb -f hls "
                                      f"{self.recordingsPath}/{location}/{location}-$(date \"+%s\")_.m3u8")
                    case CameraType.zxtechMCW5B10X.value:  # Need to prevent double speed layback with this camera
                        ffmpeg_cmd = (
                            f"ffmpeg -i {path} -t 01:00:00 -an -bsf:v h264_mp4toannexb  -vf \"setpts=2.0*N/FRAME_RATE/TB\" -f hls "
                            f"{self.recordingsPath}/{location}/{location}-$(date \"+%s\")_.m3u8")
                    case _:
                        logger.warning(f"No camera type for file {path}, deleting it")
                        os.remove(path)

            if ffmpeg_cmd != "":
                result: [int, str] = executeOsCommand(f"nice -10 {ffmpeg_cmd}")
                msg: str = result.pop()
                if msg == '':
                    os.remove(path)
                    logger.info(f"File processed {path}")
                else:
                    logger.error(f"Error {result.pop()}, {msg}")

            # Remove old directories and any remaining files created by camera FTP transfers
            executeOsCommand(f"find {self.ftpPath}/* -mtime 2 -delete")

        except TypeError as t:
            logger.error(f"Exception TypeError was raised {t!r}")
        except KeyError as k:
            logger.error(f"Exception KeyError was raised {k!r}")
        except Exception as e:
            logger.error(f"An exception occurred: {e!r}")


def main():
    authorizer = DummyAuthorizer()
    authorizer.add_user('user', '12345', homedir=FTPAndVideoFileProcessor.ftpPath, perm='elradfmwMT')
    authorizer.add_anonymous(homedir='.')

    handler = FTPAndVideoFileProcessor
    handler.authorizer = authorizer
    server = ThreadedFTPServer(('', 2121), handler)
    server.serve_forever()


if __name__ == "__main__":
    main()
