## NVR System Structure
### Run Time Platform and Installation
The current build configuration is for Raspberry pi V4 or V5 running headless (server) version of Ubuntu 24.04 (Noble Numbat).
Installation of the complete system can be done with a .deb file which you can obtain from the latest release in the Releases section or you can build yourself by following the details under the Development section below.

### Security
The NVR is designed to run on a LAN which is protected from unauthorised
external access. From within the LAN, access to administrative functions is possible without authentication.
Secure authenticated access is through ports 443 and 446 via nginx.
These ports, plus port 80,  are set up for port forwarding on the router when direct access
from outside the LAN is required.

### Tomcat Web Server
Tomcat 10 (https://tomcat.apache.org/) hosts the server (Web Back End) and client (Web Front End) of the NVR, giving access
to these through port 8080.

###### nginx makes the web server, media server and other services available through the single port 443
### Web Front End
The Web Front End (client) is an Angular application using [Angular CLI](https://github.com/angular/angular-cli) version 18.5.
This forms the user interface of the web application.
To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI Overview and Command Reference](https://angular.io/cli) page.
### Web Back End
The Web Back End (server) is a Spring Boot 3.3.4 application (https://spring.io/projects/spring-boot), which provides
the Restful API for the Angular Web Front End. It provides the services to get and set application data as
well as configuring the Camera setup.

### Media Server
Converts the RTSP video/audio output from the cameras to up three separate HTTP streams. 
* A video websocket stream consisting of plain H264/HEVC depending on the cameras output. This is used for the live video.
* An audio websocket stream (if audio is present on the camera and selected in the config). This is used for the live audio content.
* A delayed flv HTTP stream (if recording is selected for the camera). This stream contains the video and audio (if present), and is used for recording from.
nginx provides access to this service with a common origin (port 443) to the Web Back End server.

The Media Server is written in go (golang) and cross-compiled for the ARM 64 architecture of the Raspberry pi. To change to a different architecture, edit the build task in low-latency-media-server/build.gradle

### ffmpeg
ffmpeg is used for re-muxing, audio transcoding, camera connectivity and recording.

ffmpeg version 6.1.1-3ubuntu5 is used in the NVR, which is the standard version available on
Ubuntu 24.04.
See [FFMPEG.md](FFMPEG.md) for more about ffmpeg.

### Wi-Fi Setup Service
Runs as a root Linux service. It is a web application written in Python,
used to list Wi-Fi access points, list the NVR's LAN IP addresses and set up the NVR Wi-Fi and credentials.
It also stops and starts the media server, recording service and the motion service
during configuration updates.

### Camera Recordings Service
This service records a section of video in response to an FTP image upload from a camera.
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

###### Configurable from the configuration page. You can select either FTP or Motion Service triggered recording or none for any camera, but not both together.
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

