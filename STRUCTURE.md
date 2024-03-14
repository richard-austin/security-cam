## NVR System Structure
### Run Time Platform and Installation
The current build configuration is for Raspberry pi V4 or V5 running headless (server) version of Ubuntu 23.10 (Mantic Minotaur).
Installation of the complete system can be done with a .deb file which you can obtain from the latest release in the Releases section or you can build yourself by following the details under the Development section below.

### Security
The NVR is designed to run on a LAN which is protected from unauthorised
external access. From within the LAN, access to administrative functions is possible without authentication.
Secure authenticated access is through ports 443 and 446 via nginx.
These ports, plus port 80,  are set up for port forwarding on the router when direct access
from outside the LAN is required.

### Tomcat Web Server
Tomcat 9 (https://tomcat.apache.org/) hosts the server (Web Back End) and client (Web Front End) of the NVR, giving access
to these through port 8080.

###### nginx makes the web server, media server and other services available through the single port 443
### Web Front End
The Web Front End (client) is an Angular application using [Angular CLI](https://github.com/angular/angular-cli) version 12.0.5 or later.
This forms the user interface of the web application.
To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI Overview and Command Reference](https://angular.io/cli) page.
### Web Back End
The Web Back End (server) is a Grails application (https://grails.org/), which provides
the Restful API for the Angular Web Front End.
It provides the services to get and set application data as
well as configuring the Camera setup.

### Media Server
Converts the RTSP video/audio output from the cameras to
fragmented MP4 streams which are used directly by MSE on the Web Front End.
ffmpeg is used for camera RTSP connections and multiplexing to fragmented MP4, then feeding this to the media server input.
The media server supports web socket client connections through which the media streams are read. The media streams are
also available through http connections which are used for recording.
The http streams used for recording are delayed by a configurable amount to provide some preamble from before the trigger time.  

nginx provides access to this service with common origin (port 443) to the Web Back End (https port 443).

The Media Server is written in go (golang) and cross compiled for the ARM 64 architecture of the Raspberry pi. To change to a different architecture, edit the build task in fmp4-ws-media-server/build.gradle

### ffmpeg
ffmpeg version 4.4.4 precompiled for ARM64 is deployed with this system as version 5 and above have issues with
a lack of timestamps from the RTSP cameras. If you want to run this on a platform other than ARM64, you
will need to compile ffmpeg 4.4.4 for that platform (see [FFMPEG.md](FFMPEG.md)).
### Wi-Fi Setup Service
Runs as a root Linux service. It is a web application written in Python,
used to list Wi-Fi access points, list the NVR's LAN IP addresses and set up the NVR Wi-Fi and credentials.
It also stops and starts the media server, recording service and the motion service
during configuration updates.

### Camera Recordings Service
Records a section of video in response to an FTP image upload from a camera.
The ftp upload is to a specific path which the Camera Recordings Service uses
to determine which camera to initiate a recording on. The path corresponds to the cameras
cameraID.
Recordings are of minimum length 30 seconds, but extended by a further 30 seconds whenever a further FTP upload is received
before the recording is complete.
###### Cameras which can ftp an image on detecting motion may use this service. The cameras ftp client should connect to port 2121 on the NVR with credentials user and password 12345. The remote directory should be set to the camera ID (camera*n* as appropriate).
### Motion Service
Detects motion on video streams and initiates recording in response. <a href="https://github.com/Motion-Project/motion">Motion</a> is a third party project.
On this NVR, Motion can detect and record motion on one stream of each camera, (usually the lower resolution stream to keep CPU usage lower) trigger the NVR to
start a recording on another (usually the higher resolution) stream so that recordings
in both resolutions are made.

###### Configurable from the cameras configuration page. You can select either FTP or Motion Service triggered recording or none for any camera, but not both together.
### nginx
nginx (https://nginx.org/en/linux_packages.html) is a reverse proxy through which client access to all the NVR services are accessed.
Access is through a single port (443), giving them a common origin from the browser point of view.
An additional port (446) provides access to the proxy host for the camera admin web pages.

#### What nginx is used for on the NVR
* TLS encryption of all traffic.
* Translation from Tomcat port 8080 to HTTPS port 443.
* HTTP redirect from port 80.
* Webserver, live and recorded streams made available through a single port (443) at their designated URLs.
* Makes the unauthenticated live and recorded streams dependent on the web application authentication so that they
  cannot be accessed without the user having logged in.
* Access to the cameras web admin page proxy provided through port 446.

### NTP Server
The NVR runs an NTP server (https://chrony-project.org/) to provide time synchronisation for cameras without them needing to be connected
to the internet.

###### If you want to isolate cameras from their manufacturers cloud service, you can either block their IP addresses from internet access on your router, or set the camera to a fixed IP and set the default gateway to the cameras own IP address. This will leave access to the LAN, but not external addresses.
###### For the NTP time control to work, you must then set the cameras NTP server address to the NVR IP address.

### ffmpeg
ffmpeg is used for re-muxing, audio transcoding and camera connectivity. 

ffmpeg version 4 is used in the NVR as versions 5 and above will not correctly mux the rtsp to fmp4 when audio is present.
The problem is due to a lack of timestamps in one or both of the streams.
See [FFMPEG.md](FFMPEG.md) for details of this.
