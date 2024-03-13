<h2 style="text-align: center">Security Cam</h2>
<h2 id="h2" style="text-align: center">CCTV on Raspberry Pi 4 Via Web Browser</h2>

### Introduction
This is a Network Video Recorder accessed through a web browser, designed to run on a Raspberry pi.
It requires network cameras providing RTSP streams with the video encoded as H264 or H265. Audio (G711 or AAC) is supported. 
The audio and video is remultiplexed to fragmented MP4 (fMP4) for rendering on the browser using Media Source Extensions (MSE).

![ptz camera](README.images/ptz.png)
*PTZ camera as viewed with Google Chrome*


![multi cam](README.images/multi-cam.png)
*Multi camera view on Google Chrome (edited for privacy)*

#### NVR features
* Secure authenticated web access.
* Live, low latency (approx 1 second or less) video and audio.
* Supports network cameras with RTSP streams H264/H265/audio (not USB cameras).
* Onvif support for device and capabilities discovery and PTZ control.
* View live stream from individual or all cameras.
* Recordings triggered by Motion service (https://github.com/Motion-Project/motion)
*OR* by FTP of an image from camera (can be used with cameras which can ftp an image on detecting motion). 
* Recordings of motion events, selectable by date and time.
* PTZ for cameras supporting this feature through Onvif.
* Quick reboot or setup of key camera parameters for SV3C type cameras.
* Hosting of camera admin page, This allows secure access to camera web admin outside the LAN.
  This feature requires access through port 446 as well as the usual https port 443.
* Configuration editor supporting Onvif discovery of cameras and their capabilities. Cam also find capabilities of specific cameras.
* email notification if public IP address changes (for when port forwarding is used).
* Initial unauthenticated set up of user account from LAN only. Subsequent changes can be done when logged in through existing account.
* Get NVR LAN IP addresses.
* Get Local Wi-Fi source details.
* Set up Wi-Fi connection.
* NVR includes NTP server for cameras to sync time without the need for them to connect to the internet.
* Complete project deployment using a single deb file
#### Limitations
* Requires network cameras which provide H264 or 265 video, and optionally audio via RTSP (G711/AAC). *No video transcoding
is done on the raspberry pi to keep CPU utilisation low*
* The browser used must be able to display the video format used. Most browsers will support H264, but on some
older machines, the GPU may not support H265 (HEVC) decoding. There are special chromium forks which can render H265
with software decoding (see <a href="https://thorium.rocks/">Thorium</a> and <a href="https://github.com/StaZhu/enable-chromium-hevc-hardware-decoding">Special Chromium Build</a>)
  * For Chromium based browsers running on Ubuntu 23.04 with VAAPI installed and a suitable Intel GPU, you may need to use the parameters --enable-features=VaapiVideoDecodeLinuxGL,VaapiVideoDecoder,VaapiVideoEncoder in the command line to enable hevc decoding.
This will also enable hardware decoding generally.
* https web admin hosting and rstps (secure rtsp streaming from cameras) are not currently supported.
* This has been tested with SV3C and ZXTech cameras and Reolink Wi-Fi doorbell.
There might be compatibility issues with some other camera types.
* 2 way audio (Onvif profile T) supported on the Reolink Wi-fi doorbell using firmware version v3.0.0.1996_23053101.
The firmware from Reolink main downloads site does not fully support this functionality. 
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

###### nginx makes the web server and other services available through port 443
### Web Front End
The Web Front End (client) is an Angular application using [Angular CLI](https://github.com/angular/angular-cli) version 12.0.5 or later.
This forms the user interface of the web application.
To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI Overview and Command Reference](https://angular.io/cli) page.
### Web Back End
The Web Back End (server) is a Grails application (https://grails.org/), which provides
a Restful API for the Angular Web Front End.
It provides the services to get and set application data as
well as configuring the Camera setup.

### Media Server
Converts the RTSP video/audio output from the cameras to
fragmented MP4 streams which are used directly by MSE on the Web Front End.
ffmpeg is used for camera RTSP connections and multiplexing to fragmented MP4, then feeding this to the media server input. 
The media server supports web socket client connections through which the media streams are read. The media streams are
also available through http connections which are used for recording.

nginx provides access to this service with common origin (port 443) to the Web Back End (https port 443).

The Media Server is written in go (golang) and cross compiled for the ARM 64 architecture of the Raspberry pi. To change to a different architecture, edit the build task in fmp4-ws-media-server/build.gradle
### Wi-Fi Setup Service
Runs as a root Linux service. It is a web application written in Python,
used to list Wi-Fi access points, list the NVR's LAN IP addresses and set up the NVR Wi-Fi and credentials.
It also stops and starts the media server, recording service and the motion service
during configuration updates.

### ffmpeg
ffmpeg version 4 is used in the NVR as versions 5 and above will not correctly mux the rtsp to fmp4 when audio is present. 
The problem is due to a lack of timestamps in one or both of the streams, and whereas ffmpeg4
deals with it satisfactorily, version 5 and above don't want to know, giving the below error messages continuously.
<pre>
pts has no value
Packet duration: -1131 / dts: 152727 is out of range
</pre>

As ffmpeg 4 is not available as standard on Ubuntu 23.10, I have built an ffmpeg v4 executable
for ARM64 architecture (at xtrn-scripts-and-config/ffmpeg-v4.4.4) which is deployed on installation of the 
main project deb file. 

If you are deploying to a Raspberry pi, you can skip to the *Camera Recordings Service* section.

If you are deploying the NVR to a non ARM64 platform, you will need to
build ffmpeg 4 on that platform as follows:-. 


```bash
wget https://ffmpeg.org/releases/ffmpeg-4.4.4.tar.xz
tar -xvf ffmpeg-4.4.4.tar.xz
cd ffmpeg-4.4.4/
./configure
make
```
The executable ffmpeg will be in the ffmpeg-4.4.4 directory. To deploy it in the deb file with the rest of the 
NVR, copy it to the project at xtrn-scripts-and-config/ffmpeg-v4.4.4.
##### ffmpeg Build problem
On Ubuntu 23.10, I got the following compilation error when building ffmpeg 4: -
<pre>
CC      libavformat/adtsenc.o
./libavcodec/x86/mathops.h: Assembler messages:
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error:ffmpeg-4.4.4 operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
make: *** [ffbuild/common.mak:81: libavformat/adtsenc.o] Error 1
</pre>

To fix this, copy xtrn-scripts-and-config/ffmpeg-v4.4.4/mathopts.patch to
your ffmpeg-4.4.4 build directory and (from the ffmpeg-4.4.4 build directory) run

```text
patch -u -b  ./libavcodec/x86/mathops.h mathopts.patch 
```
Run 
```text
make clean
make
```

and it should now build OK.
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

## Development
Pre built .deb files for deployment on a Raspberry pi 4 are available in the Releases section. 
The requirements to build the project yourself are detailed below: -
#### Platform for Development
* Ubuntu 23.10 (Mantic Minotaur) on PC (Windows WSL2 or direct boot)

#### The project is verified to build with the following:-
* go version go1.20.1
* Angular CLI: 15.2.0 or greater
* Node: 18.17.1
* npm: 9.9.7
* Package Manager: npm 9.6.7
* Grails Version: 5.3.2
* openjdk version "19.0.2" 2023-01-17
* Gradle 7.6
* Python 3.11.4

Using other versions may cause build issues in some cases.

### Set up build environment
```
git clone git@github.com:richard-austin/security-cam.git
cd security-cam
```
### Build for deployment to Raspberry pi
The Raspberry pi should be running Ubuntu 23.10 (Mantic Minotaur) OS.
```
./gradlew buildDebFile 
```
This will create a deb file with a name of the form security-cam_*VERSION*-*nn*-*ID-dirty*_arm64.deb
Where:-
* *VERSION* is the most recent git repo tag
* *nn* Is the number of commits since the last git tag (not present if no commits since last tag.)
* *ID* The last git commit ID (not present if no commits since last tag.)
* *dirty* "dirty" is included in the name if there were uncommitted changes to the source code when built.

When the build completes navigate to where the .deb file was created:-
```
cd xtrn-scripts-and-config/deb-file-creation
```
scp the .deb file to the Raspberry pi
## Installation on the Raspberry pi
```
sudo apt update
sudo apt upgrade 
```
(restart if advised to after upgrade)

Navigate to where the .deb file is located
<pre>
sudo apt install ./<i>deb_file_name</i>.deb
</pre>
* Wait for installation to complete.
* The Tomcat web server will take 1 - 2 minutes to start
  the application.
* <i>If this is the first installation on the Raspberry pi..</i>
  * Make a note of the product key (a few lines up). 
This will be required if you use the Cloud Service to connect
to the NVR, otherwise it is not required.
  * <i>Generate the site certificate..</i>
    ```
    cd /etc/security-cam
    sudo ./install-cert.sh
    ```
    Fill in the details it requests (don't put in any information you are not happy with being publicly visible, for
    example you may want to put in a fake email address etc.)    
  * nginx will not have started in the absence of the site certificate, so restart nginx.
    ```
    sudo systemctl restart nginx
    ```
## Initial Setup
### Setup for Direct Access (Browser to NVR)
#### Set up user account
To log into the NVR when accessing it directly, 
a user account must be set up. This is done using the Create User Account
application (cua) which is accessible from the LAN without authentication. Be sure port 8080 on the Raspberry pi is not 
accessible from outside the secure LAN. cua is also available when logged
into the NVR (from "Admin Functions" on the General menu).
* From a separate device on the LAN, open a browser and go to
<a>http://<i>nvr_ip_addr</i>:8080/cua</a>
* Click on the <img src="README.images/hamburger-2-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img> icon at the top left of the page.
* Select "Create or Update User Account" from the menu.
* Enter the required username.
* Enter the password, then again in Confirm Password
* Enter the email address you will use for forgotten password etc.
* Enter email again in Confirm email address.
* Click Update Account to confirm
#### Setup SMTP email Client
The email address set up in the previous section is where warning emails 
are sent if the public IP address changes (when NVR is used on an
internet connection with dynamic IP), or for reset password links to
be sent when password is forgotten. To do this,
the NVR email client must be logged into an SMTP client
* Click on the <img src="README.images/hamburger-2-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img> icon at the top left of the page.
* Select Set Up "SMTP Client" from the menu.
* If the SMTP connection is to be authenticated (normally the case)...
  * Check the Authenticated checkbox.
  * Enter the SMTP password.
  * Enter the SMTP password again to confirm.
* If TLS encryption is to be used (normally the case)...
  * Check the TLS Encrypted checkbox.
  * Enter the host name for the SMTP client to trust 
(normally the SMTP host name).
* Enter the SMTP host name
* Enter the SMTP port
* Enter the "from" (sender) address these email will appear to come from
* Click confirm.

## Login to the NVR
* Set a browser to <a>https://<i>nvr_ip_addr</i></a>
* Ignore the warning which may be given as a result of the home generated
site certificate and continue to the log in dialogue box.
* Enter the username and password set up under "Set up user account". 
You can check "Remember me" to enable auto login in the future.
* Click the SIGN IN button
* You are now logged into your NVR and should see a menu bar at the top of a blank page.
## Set up Wi-Fi (if required)
If you want to use Wi-Fi, and it hasn't previously been set up on the Raspberry pi, it can be done from the NVRs 
Wifi Admin -> Wifi Settings page. Note
that Wi-Fi settings can only be changed when the Raspberry pi (NVR) is connected to the LAN and you are accessing the web server
through the Ethernet (eth0 on Raspberry pi) IP address.
* Connect a browser to the NVRs Ethernet LAN address.
* From the menu select General -> Wifi Admin -> Wifi Settings.
* Select your Wi-Fi access point on the dropdown
* Click connect.
* Enter the password if required.
* Click connect.

The Wi-Fi will now be set up, You can check the IP address for the Wi-Fi Connection using the General -> 
Get Active Local IP Address(es) option.

## Set Up Cameras
The NVR must be configured to use your cameras.
The configuration editor can be found at General ->
Cameras Configuration.

If camera configuration has not yet been done, the Cameras Configuration Editor will
be empty apart from a single unpopulated camera entry.

![config editor](README.images/config-editor.png)
*Cameras Configuration Editor*

#### Config page button functions
<img src="README.images/security-svgrepo-com.svg" title="Image of camera configuration page" width="20"/></img> **At top of page to the left of the 
page title.**

Set the global Onvif credentials used during single and all camera Onvif discovery. Cameras successfully discovered
will have their credentials set to those entered here. Cameras whose credentials were different from these, will
be listed above the table*
allowing the correct credentials to be entered against them before
retrying discovery on them individually.

&ast; See *Cameras Configuration Editor Showing A Camera Whose Onvif Credentials Differ From The Global Onvif Credentials* below. 

| Button                                                                                                                                   | Function                                                                                                                                                                                                                                                                                                                                                                                    |
|------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| <img src="README.images/simple-trash-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"/></img> (on&nbsp;camera&nbsp;row) | Delete the corresponding camera and its streams. Disabled when there is only one camera                                                                                                                                                                                                                                                                                                     |
| <img src="README.images/simple-trash-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img> (on&nbsp;stream&nbsp;row)  | Delete the corresponding stream. Disabled when the stream is the only one on the camera.                                                                                                                                                                                                                                                                                                    |
| <img src="README.images/add-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img>                                     | Add a new stream. The new stream will be unpopulated and all fields will need manual entry/setup.                                                                                                                                                                                                                                                                                           |
| <img src="README.images/arrow-sm-down-svgrepo-com.svg" title="Down arrow" width="20"/></img>                                             | Move the corresponding camera down one place in the list. The cameras will be listed on the selection menus in the same order as they appear on this list.                                                                                                                                                                                                                                  |
| <img src="README.images/arrow-sm-up-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img>                             | Move the corresponding camera up one place in the list. The cameras will be listed on the selection menus in the same order as they appear on this list.                                                                                                                                                                                                                                    |                                                                                                                                                                  |
| <img src="README.images/add-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img>                                     | Add a new camera. This will add a camera with one stream, with all fields unpopulated. All fields will need to be populated manually.                                                                                                                                                                                                                                                       |
| <img src="README.images/add-circle-solid-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img>                        | Add a new camera. You enter the Onvif URL for the required camera, and the camera details will be returned with camera specific data populated. Intended for when General Onvif Discovery has not picked up the camera or a new camera is added to an existing setup. This is the preferred way to add a single camera. You will need to enter the camera name and stream ID's as a minimum |
| <img src="README.images/blank-document-svgrepo-com.svg" width="20" style="transform: rotate(90deg); position: relative; top: 5px"></img> | Start a new configuration. After confirmation, any camera data will be cleared and a single unpopulated camera/stream will be added. You will need to populate all the camera and stream fields, manually.                                                                                                                                                                                  |
| <img src="README.images/compass-circular-tool-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img>                   | General Onvif discovery. After confirmation, the Onvif function will try to discover cameras on the network. Any that are found will have most of their characteristics populated.                                                                                                                                                                                                          |
| <img src="README.images/floppy-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img>                                  | Save configuration. Any changes made with the editor will only become active after saving with this function. Make sure to use this after making any updates.                                                                                                                                                                                                                               |
| <img src="README.images/caret-right-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img>  &ast;                      | Show the cameras streams                                                                                                                                                                                                                                                                                                                                                                    |
| <img src="README.images/caret-bottom-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img> &ast;                      | Hide the cameras streams                                                                                                                                                                                                                                                                                                                                                                    |
| camera(<i>n</i>)                                                                                                                         | Camera ID. Click on this to show a snapshot from the camera. Note that this will require that the camera credentials are set up correctly (<img src="README.images/security-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img> button)                                                                                                                                |
| <img src="README.images/security-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img>                                | Enter or change credentials for this camera. These credentials will be used on view snapshot (on config setup), on camera settings and admin page hosting (SV3C and ZXTech cameras only) and in RTSP authentication (when selected).                                                                                                                                                        |

&ast; Button style toggles with context

### Onvif
From https://github.com/fpompermaier/onvif

The NVR supports Onvif camera discovery and population of parameters. This should be used
when supported by your cameras. Click on the <img src="README.images/compass-circular-tool-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img> button
(Perform onvif LAN search for cameras) to locate cameras on the LAN. Before you can save
the configuration you need to complete any missing fields (typically
the camera names and stream descriptions). When done, click
on the <img src="README.images/floppy-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img> button to commit the current configuration

#### Camera not found
If any cameras do not respond to the multicast probe, they will not
be listed after Onvif discovery. Where the camera supports Onvif, you can
search for individual cameras by their Onvif URL
Click the <img src="README.images/add-circle-solid-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"> button, enter the Onvif URL (for example http://192.168.1.43:8080/onvif/device_service, where the IP
address is the IP of the camera). This will add the parameters
for the specified camera to the list. You then just need to complete the name and description fields and recording settings etc.

Cameras can also be added manually by clicking on the <img src="README.images/add-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img> button
. In this case you will have to enter all parameters yourself, so it's
not recommended unless Onvif is not supported on the device.

#### Camera Configuration Parameters
| Parameter/Control   | Function                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     | Set by Onvif Discovery |
|---------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------|
| Sorting             | Up and down arrows move camera position in the list, and correspondingly on the menus.                                                                                                                                                                                                                                                                                                                                                                                                                                                       | N/A                    |
| Camera ID           | Map key of the camera. Clicking on this displays a snapshot from that camera.                                                                                                                                                                                                                                                                                                                                                                                                                                                                | N/A                    |
| Cam Credentials     | Enter or change the credentials used for this camera. This is set to the global onvif credentials if this camera was successfully discovered with Onvif, but can be changed with this function if required. <br/><br/> NOTE: You must save save the configuration (using the <img src="README.images/floppy-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img> button) to make the credentials stick.<br/><br/>Before saving, you can click on the camera ID to check the (usually authenticated) snapshot is working. | N/A                    |
| Delete              | Delete this camera and its streams from the configuration (trash button).                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | N/A                    |
| Name                | The name of the camera as it will appear on the menus.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | No                     |
| Camera Type         | Select SV3C, ZTech MCW5B10X or Not Listed. The named options enable some admin functions under Camera Settings -> Quick camera Setup                                                                                                                                                                                                                                                                                                                                                                                                         | No                     |
| FTP From camera     | If a stream is selected, recording will be triggered when the camera sends a .jpg (jpeg) to ./<i>camera_map_key</i>  on the NVR IP address on port 2121. This is not available if Motion Sensing is set on any of the camera streams. If 'none' is selected, ftp uploads will not be processed.                                                                                                                                                                                                                                              | No                     |
| Retrigger Window    | For FTP triggered recordings only. The time window in seconds during which the recording can be extended (by retrigger window seconds) by further FTP uploads from the camera.                                                                                                                                                                                                                                                                                                                                                               | No                     |
| Address             | Camera IP address                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            | Yes                    |
| Snapshot URI        | The cameras snapshot URL.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | Yes                    |
| RTSP Authentication | If checked, authentication will be used on the RTSP connection to the camera. The credentials will be those entered for the camera set on the Cameras Config page.                                                                                                                                                                                                                                                                                                                                                                           | No                     |
| RTSP Transport      | Determine whether to use TCP or UDP for the RTSP video/audio stream. If in doubt, use TCP.                                                                                                                                                                                                                                                                                                                                                                                                                                                   | No                     |
| Audio Backchannel   | Enable use of the cameras Audio backchannel for two way audio (if camera supports Onvif Profile T backchannel). (<img src="README.images/xmark-circle-svgrepo-com.svg" width="20" style="position: relative; top: 5px"> inactive, <img src="README.images/tick-circle-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img> active. Click to toggle).</img>                                                                                                                                                               | Yes                    |
| PTZ Controls        | Enable PTZ controls on the live stream view. This requires that the camera supports Onvif PTZ control.                                                                                                                                                                                                                                                                                                                                                                                                                                       | No                     |
| Onvif Base Address  | IP address and port of the cameras Onvif SOAP web service.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | Yes                    |

#### Stream Parameters
| Parameter/Control        | Function                                                                                                                                                                                                                                                                                                                                                                                                                       | Set by Onvif Discovery |
|--------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------|
| Stream ID                | Map key of the stream                                                                                                                                                                                                                                                                                                                                                                                                          | N/A                    |
| Delete                   | Delete this stream from the camera.                                                                                                                                                                                                                                                                                                                                                                                            | N/A                    |
| Descr.                   | Description of the stream (typically HD/SD). This is appended to the camera name on the menus.                                                                                                                                                                                                                                                                                                                                 | No                     |
| Audio                    | Check to include the cameras audio with the video (if present).                                                                                                                                                                                                                                                                                                                                                                | No                     |
| Audio Encoding           | Set to the audio encoding on the cameras RTSP stream. If the audio format is AAC, it will be passed through as is, otherwise it will be encoded to AAC.                                                                                                                                                                                                                                                                        | Yes                    |
| Netcam URI               | The cameras RTSP video/audio url.                                                                                                                                                                                                                                                                                                                                                                                              | Yes                    |
| Default On Multi Display | Sets the stream as this cameras default on the Multi Camera View. The multi camera view allows switching to other streams than the default.                                                                                                                                                                                                                                                                                    | N/A                    |
| Motion Sensing           | If checked, the motion service will be used to detect motion from this stream. To keep CPU usage down, it's best to select a lower resolution stream. Not available if FTP is selected on the camera.                                                                                                                                                                                                                          | N/A                    |
| Motion Threshold         | See Motion project documentation <a href="https://motion-project.github.io/motion_config.html#threshold">here</a>                                                                                                                                                                                                                                                                                                              | N/A                    |
| Trigger Recording On     | When Motion Sensing is selected for the stream, you can select another (usually higher resolution) stream to record from in addition to the stream used for motion detection. Both streams will be selectable on the Select Recording menu.                                                                                                                                                                                    | N/A                    |
| Preamble Frames          | Used for both motion and FTP triggered recordings (not the actual recordings made by motion service). This is the number of frames to delay the stream by when making the recording. The recording will then contain a period of activity before the point at which it was triggered. Note that frames also include audio frames, so the if the stream includes audio, this may need to be a higher number for the same delay. | N/A                    |
| Mask File                | Select a mask file for this stream in the motion service. (see https://motion-project.github.io/motion_config.html#mask_file).                                                                                                                                                                                                                                                                                                 | N/A                    |
| Video Width              | For motion Service, the width of the video stream in pixels (see https://motion-project.github.io/motion_config.html#width)                                                                                                                                                                                                                                                                                                    | Yes                    |
| Video Height             | For Motion Service, the height of the video stream in pixels (see https://motion-project.github.io/motion_config.html#height)                                                                                                                                                                                                                                                                                                  | Yes                    |

![config editor](README.images/config-editor2.png "Camera configuration page following Onvif discovery, showing a camera whose credentials are different from the global Onvif credentials")
*Cameras Configuration Editor Showing A Camera Whose Onvif Credentials Differ From The Global Onvif Credentials*

## One or More Cameras Failing Onvif Authentication During Discovery
### Cameras in this category will be listed in the orange bordered box shown in the above screenshot.
Enter the correct Onvif user name and password for the camera listed in the orange bordered box and click the <img src="README.images/add-circle-solid-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img>
button beside the password to run single camera discovery using these credentials. If successful, the camera will be removed from this list and added 
tol the cameras list below, where parameter entry can be completed. Once all parameters are set, the new configuration must be saved 
(<img src="README.images/floppy-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img> button) to take effect.
## Using the NVR

### The Menus 

The NVR has a menu bar at the top of the page. On a PC screen
this menu bar will normally show the menu names, though on a mobile
device a <img src="README.images/hamburger-2-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img> icon must be tapped to reveal them.

Before any option is selected, the
page below the menu bar will be blank.

#### Select Camera
This menu allows selection of live video/audio camera streams. The names
are listed in the form <i>Camera Name(Stream Description)</i>
so there can be more than one stream per camera.
  
> On selecting the live stream, the video will be shown with a 
<img src="README.images/stopwatch-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img>
icon and a selector just below it. The selection option is the maximum
> latency in seconds. You can set this to the lowest setting where the video remains stable. 
> The lowest usable setting is dependent on various factors and won't
> necessarily be the same for all your cameras.
> 
> If two-way audio is enabled, a <img src="README.images/microphone-off-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img>
> button and a device selector will also be below the video. Select the required audio input
> device and click the <img src="README.images/microphone-off-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img> button to
> speak. While audio output is active, the button changes to <img src="README.images/microphone-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img>,
> click it again to turn audio output off.
> 
> You can zoom in on the video by using the mouse wheel on a PC, or pinch zoom on a mobile. Mouse down and drag will pan the zoomed video while
> touch and move does the same on mobile. This is distinct from PTZ operations which can be done on suitable cameras.

##### Multi Camera View
The last option on the Select Camera menu is Multi Camera View. This shows one stream
from each camera in the configuration. The default stream shown
for a camera will be the one selected as Default On Multi Display.
The camera streams can be switched by clicking the
<img src="README.images/settings-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img>
button at the top left of the page and changing the selection.
> Each video on multi camera view will have the latency chasing setter below it. Any
> cameras with two-way audio enabled will also have the <img src="README.images/microphone-off-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img> button and device selector.
> 
> Mouse wheel zoom and mouse down drag pan functions, and their mobile counterparts can be done on each of the videos.
#### Select Recording
This menu allows selection of recordings made on camera streams.
The names
are listed in the form <i>Camera Name(Stream Description)</i>.
On selection, the latest recording on that stream will be shown. 
Earlier recordings can be selected from the date control and Motion
Events selector in the top left of the page. A warning box will appear if there are no recordings for the selected stream.

#### Camera Settings
* **Quick Camera Setup**

  <i>Quick Camera Setup is available only for SV3C and ZXTech cameras (as set in the 
configuration for Camera Type)</i>

  This provides a convenient means of setting the night lighting mode, and Camera Name.
  The camera can also be rebooted if required.
* **Camera Admin** 

  Note that for this function to be accessible outside the LAN, port forwarding must be set up for port 446.

  Provides access to camera web admin page through the NVR. Access is 
protected by the NVR authentication system (Spring Security) as well as the NVR's access token system. SV3C 
and ZXTech camera credentials are provided by the NVR if they
were set up in the camera configuration page, otherwise they will need to be entered after selecting the camera.
Any other camera types will need their credentials entered after the camera is selected.

#### General
* **Configure Camera Setup**

  Set up the NVR for your set of cameras (which must be on the same LAN as the NVR).

  This is described in more detail under the section "Set Up Cameras" above.
* **Log Off**

  Log off from the NVR, a dialogue box allows confirmation or cancelling this operation.
* **Change Password**

  User must first enter the current password, then enter and confirm a new one.
* **Change Account Email**

  The email address is where password reset links will be sent when requesting from the login page. It is also where warning messages are sent if the public IP address has changed (for use with external access via port forwarding).

  User must first enter the password, then enter and confirm the new email address.
* **Set Up Guest Account**

  The guest account allows viewing the live streams and recordings, but does not allow any sort of admin operation which could alter the configuration.

  Enter and confirm a password for the guest account. This password
is used with the username "guest" to log in. The checkbox allows the guest account to be enabled and disabled, while the password remains unaltered.
 
* **Save current IP**
  
  Save the current public IP that the NVR appears at (with port forwarding set up).

  If the public IP subsequently changes from this, a warning email will be sent to the registered 
email address, giving the new public IP address.

  This function is only used after setting up a new NVR or after the public IP has changed.
* **Get Active Local IP Addresses**

  Get the LAN addresses of the NVR. There are IP addresses for the Wi-Fi and Ethernet interfaces.
* **Wifi Admin**
  * **Local Wifi Details**

    Lists the Wi-Fi access points in the area along with signal strength and other information.
  * **Wifi Settings**

    Setup or enable/disable the NVR Wi-Fi connection.

    To use this the NVR must have an Ethernet connection, and the browser must be connected to the NVR through the Ethernet IP address. 
* **Set CloudProxy Status**

**Note, An experimental Cloud Service has been developed (see <a href="https://github.com/richard-austin/cloud-server">here</a>). This works in conjunction with <a href="https://github.com/richard-austin/activemq-for-cloud-service">ActiveMQ</a>**
For information on accessing the NVR through the Cloud Service, see README_CLOUD.md
* **Admin Functions**
  
  The initial setup functions used to set credentials for a direct access account.
This can be accessed on the LAN without authentication at http://nvr_lan_ip_addr:8080/cua/.

  Click on the <img src="README.images/hamburger-2-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img> icon at the top left for the menu options·
  * **Create or Update User Account**

    When accessed via the General -> Admin Functions menu, a user account will already exist. 
    In this case you can use this to modify the username and/or password and/or email address for the account.
In any case, all those fields must be entered before clicking Update Account.
  * **Set Up SMTP Client**

    The SMTP Client must be set up for the NVR to send emails (reset password and changed public IP notification).

    Use this if you need to set up or change the settings for the NVR's SMTP client.
    All fields must be entered before clicking Confirm. Any existing values will be
    shown in their fields.
* **About**

  Version information for the NVR and open source software used in the project..
