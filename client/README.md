<h2 style="text-align: center">NVR for CCTV Access Via Web Browser</h2>

### Introduction
This is a Network Video Recorder accessed through ta web browser. designed to run on a Raspberry pi.
Access can be either direct or through a Cloud service. There is no live implementation
of the Cloud Service, but the source code is freely available at
https://github.com/richard-austin/cloud-server.

#### NVR features
* Secure authenticated web access.
* Live, low latency (approx 1 second) video from network cameras with RTSP source.
* View individual or all cameras on one page.
* Recordings of motion events, selectable by date and time.
* Recordings triggered by Motion service (https://github.com/Motion-Project/motion), or by FTP of an image from camera. Many cameras can ftp an image when they detect motion.
* Quick setup of certain camera parameters for SV3C type cameras.
* Hosting of camera admin page, This allows secure access to camera web admin outside the LAN.
* Configuration editor supporting Onvif camera discovery.
* email notification if public IP address changes (when using port forwarding).
* Initial set up of user account from LAN only. Subsequent changes can be done when logged in through existing account.
* Get NVR LAN IP addresses.
* Get Local Wi-Fi details.
* Set/unset NVR Wi-Fi access.
* Enable/Disable access through Cloud server.
* All parts of project and dependencies deployed using deb file.

# Web Front End
The Web Front End (client) is an Angular application using [Angular CLI](https://github.com/angular/angular-cli) version 12.0.5 or later.
To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI Overview and Command Reference](https://angular.io/cli) page.

# Web Back End
The Web Back End (server) is a Grails application which provides
a Restful API for the Angular Web Front End. 
It provides the calls to get and set application data as
well as configuring the Camera setup.

# Media Server
This provides a fragmented MP4 stream for each camera which forms the source for 
the Media Source Extensions video implementation used on the Web Front End.
ffmpeg connects to a camera RTSP output and converts that to fmp4 which can optionally include the audio stream.

The Media Server is written in go (golang) and cross compiled for the ARM 64 architecture of the Raspberry pi.
# Wi-Fi Setup Service
This runs as a Linux service with root access. It is aa web application written in Python,
used to list Wi-Fi access points, list the NVR's LAN IP addresses and set up the NVR Wi-Fi and credentials.
It also can stop and start the media server, recording service and the motion service
during configuration updates.

