# Contents
[Install nginx and mod-rmtp	 PAGEREF _Toc66375166 \h 1](#_Toc66375166)

[Set up processrecordings.sh containing this	 PAGEREF _Toc66375167 \h 4](#_Toc66375167)

[Install ffmpeg	 PAGEREF _Toc66375168 \h 5](#_Toc66375168)

[Stream from USB webcam with ffmpeg	 PAGEREF _Toc66375169 \h 5](#_Toc66375169)

[Stream from IP cameral with ffmpeg	 PAGEREF _Toc66375170 \h 5](#_Toc66375170)

[Install OBS	 PAGEREF _Toc66375171 \h 5](#_Toc66375171)

[Install v4l2 utils	 PAGEREF _Toc66375172 \h 5](#_Toc66375172)

[Compile ffmpeg to use Nvidia GPU	 PAGEREF _Toc66375173 \h 9](#_Toc66375173)

[Using ffmpeg to prepare recordings for VOD	 PAGEREF _Toc66375174 \h 9](#_Toc66375174)

[Appending the m3u8 manifest files from different preparation sessions	 PAGEREF _Toc66375175 \h 9](#_Toc66375175)

[Python command for http server (to show recordings after preparation with ffmpeg)	 PAGEREF _Toc66375176 \h 11](#_Toc66375176)

[Development with dotnet	 PAGEREF _Toc66375177 \h 12](#_Toc66375177)

[RTSP data for WIFI camera	 PAGEREF _Toc66375178 \h 12](#_Toc66375178)

[Install Motion	 PAGEREF _Toc66375179 \h 12](#_Toc66375179)

[Install ntp server	 PAGEREF _Toc66375180 \h 12](#_Toc66375180)

[Install Tomcat	 PAGEREF _Toc66375181 \h 12](#_Toc66375181)


# Install nginx and mod-rmtp
 #### This didn't work with Ubuntu 21.04 (the rtmp module was not active in nginx)
sudo apt install nginx
sudo apt install libnginx-mod-rtmp
#### So I used the following to build it
sudo apt update
sudo apt install build-essential git
sudo apt install libpcre3-dev libssl-dev zlib1g-dev
cd /path/to/build/dir
git clone https://github.com/arut/nginx-rtmp-module.git
git clone https://github.com/nginx/nginx.git
cd nginx
./auto/configure --add-module=../nginx-rtmp-module
make
sudo make install

Make install puts it at /usr/local/nginx

austinr@ubunturdp:~$ sudo cat /etc/nginx/nginx.conf

user www-data;

worker\_processes auto;

pid /run/nginx.pid;

include /etc/nginx/modules-enabled/\*.conf;

events {

`	`worker\_connections 768;

`	`# multi\_accept on;

}

rtmp { 

`    `server { 

`        `listen 1935; 

`        `application live { 

`            `live on; 



`            `exec\_options on; 

`            `exec\_static ffmpeg -re -rtsp\_transport tcp -i rtsp://192.168.0.30:554/11 -c copy -c:a aac -b:a 160k -ar 44100 -f flv rtmp:192.168.56.108/live/porch;



`            `#record all;

`            `#record\_path /home/austinr/nginxrecordings/hls;

`            `#record\_unique on;

`            `#record\_suffix -%d-%b-%y-%T.flv;

`            `#record\_interval 2m; 

`            `# Run the script to create the video on demand files from the recording file, then delete the recording file and delete the oldest VOD files

`            `exec\_record\_done /home/austinr/nginxrecordings/processrecordings.sh $path $dirname/$basename\_.m3u8 $dirname; 



`            `interleave on;

`            `hls on; 

`            `hls\_path /tmp/hls; 

`            `hls\_fragment 15s; 

`	   `#dash on;

`           `#dash\_path /tmp/dash;

`           `#dash\_fragment 15s;

`        `recorder live\_rec {

`            `record all;

`            `record\_unique on;

`            `record\_path /home/austinr/nginxrecordings/hls;

`            `record\_suffix .flv;

`            `record\_interval 10m; 

`            `#record\_max\_size 2m;

`            `}  

`        `} 

`    `} 

} 



http { 

`    `default\_type application/octet-stream;



`    `server {

`        `listen 80; 

`        `location / { 

`            `add\_header "Access-Control-Allow-Origin"  "\*" always;

`            `root /tmp; 

`        `} 

`    `}



`    `types {

`        `application/vnd.apple.mpegurl m3u8;

`        `video/mp2t ts;

`       `# text/html html;

`       `# application/dash+xml mpd;

`    `} 

}


#mail {

\#	# See sample authentication script at:

\#	# http://wiki.nginx.org/ImapAuthenticateWithApachePhpScript

\# 

\#	# auth\_http localhost/auth.php;

\#	# pop3\_capabilities "TOP" "USER";

\#	# imap\_capabilities "IMAP4rev1" "UIDPLUS";

\# 

\#	server {

\#		listen     localhost:110;

\#		protocol   pop3;

\#		proxy      on;

\#	}

\# 

\#	server {

\#		listen     localhost:143;

\#		protocol   imap;

\#		proxy      on;

\#	}

#}
## Set up processrecordings.sh containing this
ffmpeg -i $1 -c copy -level 3.0 -start\_number 0 -hls\_time 10 -hls\_list\_size 0 -f hls $2

\# Processing done so remove the nginx generated recording file

rm $1

\# Remove oldest files

find $3 -mmin +1440 | xargs rm

echo $1 $2 $3 > /home/austinr/nginxrecordings/didit.txt
# Install ffmpeg

sudo apt install ffmpeg

# Stream from USB webcam with ffmpeg
ffmpeg -i /dev/video0 -vcodec libx264 -loop -1 -c:a aac -b:a 160k -ar 44100 -strict -2 -vf "drawtext=fontfile=FreeSerif.ttf:fontcolor=white:text='%{pts\\:localtime\:`date +%s`\:%d-%m-%Y %T}:fontsize=14:x=10:y=480-24-max\_glyph\_a'[out]" -f flv rtmp:192.168.56.108/live/bbb
# Stream from IP cameral with ffmpeg
ffmpeg -rtsp\_transport tcp -i rtsp://username:password@192.168.0.30:554/12 -vcodec libx264 -loop -1 -c:a aac -b:a 160k -ar 44100 -strict -2 -vf "drawtext=fontfile=FreeSerif.ttf:fontcolor=white:text='%{pts\\:localtime\:`date +%s`\:%d-%m-%Y %T}:fontsize=14:x=10:y=480-24-max\_glyph\_a'[out]" -f flv rtmp:192.168.56.108/live/bbb

Now using without video conversion as the IP cam produces H.264 output.

ffmpeg -re -rtsp\_transport tcp -i rtsp://192.168.0.30:554/11 -c copy -c:a aac -b:a 160k -ar 44100 -f flv rtmp:192.168.0.31:1936/live/cam2;

Launched by nginx using exec\_static

The camera has its own date/time
# Install OBS 
sudo snap install obs-studio

# Install v4l2 utils

sudo apt-get install v4l-utils

Then you can use things like:-

austinr@ubunturdp:~$ v4l2-ctl --list-devices

VirtualBox Webcam - HP Truevisi (usb-0000:00:06.0-2):

`	`/dev/video0

`	`/dev/video1

And:-

austinr@ubunturdp:~$ sudo v4l2-ctl --device=/dev/video0 --all

Driver Info:

`	`Driver name      : uvcvideo

`	`Card type        : VirtualBox Webcam - HP Truevisi

`	`Bus info         : usb-0000:00:06.0-2

`	`Driver version   : 5.4.78

`	`Capabilities     : 0x84a00001

`		`Video Capture

`		`Metadata Capture

`		`Streaming

`		`Extended Pix Format

`		`Device Capabilities

`	`Device Caps      : 0x04200001

`		`Video Capture

`		`Streaming

`		`Extended Pix Format

Media Driver Info:

`	`Driver name      : uvcvideo

`	`Model            : VirtualBox Webcam - HP Truevisi

`	`Serial           : 2c7160c72b7671ad

`	`Bus info         : usb-0000:00:06.0-2

`	`Media version    : 5.4.78

`	`Hardware revision: 0x00000100 (256)

`	`Driver version   : 5.4.78

Interface Info:

`	`ID               : 0x03000002

`	`Type             : V4L Video

Entity Info:

`	`ID               : 0x00000001 (1)

`	`Name             : VirtualBox Webcam - HP Truevisi

`	`Function         : V4L2 I/O

`	`Flags         : default

`	`Pad 0x01000007   : 0: Sink

`	  `Link 0x0200000d: from remote pad 0x100000a of entity 'Processing 2': Data, Enabled, Immutable

Priority: 2

Video input : 0 (Camera 1: ok)

Format Video Capture:

`	`Width/Height      : 640/480

`	`Pixel Format      : 'MJPG' (Motion-JPEG)

`	`Field             : None

`	`Bytes per Line    : 0

`	`Size Image        : 1228800

`	`Colorspace        : sRGB

`	`Transfer Function : Default (maps to sRGB)

`	`YCbCr/HSV Encoding: Default (maps to ITU-R 601)

`	`Quantization      : Default (maps to Full Range)

`	`Flags             : 

Crop Capability Video Capture:

`	`Bounds      : Left 0, Top 0, Width 640, Height 480

`	`Default     : Left 0, Top 0, Width 640, Height 480

`	`Pixel Aspect: 1/1

Selection Video Capture: crop\_default, Left 0, Top 0, Width 640, Height 480, Flags: 

Selection Video Capture: crop\_bounds, Left 0, Top 0, Width 640, Height 480, Flags: 

Streaming Parameters Video Capture:

`	`Capabilities     : timeperframe

`	`Frames per second: 15.000 (15/1)

`	`Read buffers     : 0

`                     `brightness 0x00980900 (int)    : min=0 max=100 step=1 default=50 value=50

And:-

austinr@ubunturdp:~$ sudo v4l2-ctl --device=/dev/video1 --all

Driver Info:

`	`Driver name      : uvcvideo

`	`Card type        : VirtualBox Webcam - HP Truevisi

`	`Bus info         : usb-0000:00:06.0-2

`	`Driver version   : 5.4.78

`	`Capabilities     : 0x84a00001

`		`Video Capture

`		`Metadata Capture

`		`Streaming

`		`Extended Pix Format

`		`Device Capabilities

`	`Device Caps      : 0x04a00000

`		`Metadata Capture

`		`Streaming

`		`Extended Pix Format

Media Driver Info:

`	`Driver name      : uvcvideo

`	`Model            : VirtualBox Webcam - HP Truevisi

`	`Serial           : 2c7160c72b7671ad

`	`Bus info         : usb-0000:00:06.0-2

`	`Media version    : 5.4.78

`	`Hardware revision: 0x00000100 (256)

`	`Driver version   : 5.4.78

Interface Info:

`	`ID               : 0x03000005

`	`Type             : V4L Video

Entity Info:

`	`ID               : 0x00000004 (4)

`	`Name             : VirtualBox Webcam - HP Truevisi

`	`Function         : V4L2 I/O

Priority: 2

Format Metadata Capture:

`	`Sample Format   : 'UVCH' (UVC Payload Header Metadata)

`	`Buffer Size     : 1024

# Compile ffmpeg to use Nvidia GPU

<https://docs.nvidia.com/video-technologies/video-codec-sdk/ffmpeg-with-nvidia-gpu/>

# Using ffmpeg to prepare recordings for VOD

ffmpeg': ffmpeg -i file:bbb-1612431896-04-Feb-21-09:44:56.flv -profile:v high422 -level 3.0 -s 640x480 -start\_number 0 -hls\_time 10 -hls\_list\_size 0 -f hls ~/nginxrecordings/testfile\_out.m3u8

probably better to use

ffmpeg -i file:bbb-1612431896-04-Feb-21-09:44:56.flv -profile:v high444 -level 3.0 -start\_number 0 -hls\_time 10 -hls\_list\_size 0 -f hls ~/nginxrecordings/testfile\_out.m3u8

Better still as no video conversion dom

ffmpeg -i file:hls/bbb-1612789798-08-Feb-21-13:09:58.flv -c copy -level 3.0 -start\_number 0 -hls\_time 10 -hls\_list\_size 0 -f hls ~/nginxrecordings/testfile\_out.m3u8

# Appending the m3u8 manifest files from different preparation sessions
Place a #EXT-X-DISCONTINUITY tag at the end of the file to be appended to, then take the sequence of entries minus the header entries from the file to be appended and add them after the #EXT-X-DISCONTINUITY tag in the first file. The first file will now allow playing of the segments from the two preparation sessions (see  REF \_Ref63329527 \h Using ffmpeg to prepare recordings for VOD). Be sure to make the output file names from the preparation differ in some way, perhaps using a time stamp.

#EXTM3U

#EXT-X-VERSION:3

#EXT-X-TARGETDURATION:17

#EXT-X-MEDIA-SEQUENCE:0

#EXTINF:16.666667,

testfile\_out0.ts

#EXTINF:8.333333,

testfile\_out1.ts

#EXTINF:8.333333,

testfile\_out2.ts

#EXTINF:8.333333,

testfile\_out3.ts

#EXTINF:8.333333,

testfile\_out4.ts

#EXTINF:16.666667,

testfile\_out5.ts

#EXTINF:8.333333,

testfile\_out6.ts

#EXTINF:8.333333,

testfile\_out7.ts

#EXTINF:8.333333,

testfile\_out8.ts

#EXTINF:8.333333,

testfile\_out9.ts

#EXTINF:16.666667,

testfile\_out10.ts

#EXTINF:8.333333,

testfile\_out11.ts

#EXTINF:7.133333,

testfile\_out12.ts

#EXT-X-DISCONTINUITY

#EXTINF:16.666667,

testfilex\_out0.ts

#EXTINF:8.333333,

testfilex\_out1.ts

#EXTINF:8.333333,

testfilex\_out2.ts

#EXTINF:8.333333,

testfilex\_out3.ts

#EXTINF:8.333333,

testfilex\_out4.ts

#EXTINF:16.666667,

testfilex\_out5.ts

#EXTINF:8.333333,

testfilex\_out6.ts

#EXTINF:8.333333,

testfilex\_out7.ts

#EXTINF:8.333333,

testfilex\_out8.ts

#EXTINF:8.333333,

testfilex\_out9.ts

#EXTINF:16.666667,

testfilex\_out10.ts

#EXTINF:8.333333,

testfilex\_out11.ts

#EXTINF:6.633333,

testfilex\_out12.ts

#EXT-X-ENDLIST

# Python command for http server (to show recordings after preparation with ffmpeg)

python -m http.server 8080

(python 3, otherwise python -m SimpleHTTPServer 8080)
#
# Development with dotnet
dotnet run  //Run in dev mode

dotnet watch run   // Run in dev mode with watch for file changes triggering rebuild.

dotnet publish -o bin/release   // Production build to bin/release

dotnet security-cam.dll  // Run production build of application

# RTSP data for WIFI camera 
1.RTSP data 

main stream：rtsp://ip address:554/11 

vice stream：rtsp://ip address:554/12
# Install Motion
git clone <https://github.com/Motion-Project/motion.git>

cd motion

wget <https://raw.githubusercontent.com/Motion-Project/motion-packaging/master/builddeb.sh>

./builddeb.sh motion <a.a@xmal.com>

sudo apt install ./focal\_motion\_4.3.1+git20210125-34091a7-1\_amd64.deb

motion

Configs are at /etc/motion/motion.conf

# Install ntp server (Chrony)
As the cameras are blocked from accessing the internet, they cannot set their time from their default ntp server so running a local one on the pi and pointing the cameras at that solves the problem.

sudo apt update

sudo apt install chrony

A suitable pool of ntp servers can be set in /etc/chrony/chrony.conf if required

# Install Tomcat

Step 1: Install Java

Tomcat 9 requires Java SE 8 or later to be installed on the server before we start installation. We will install OpenJDK 11 open-source Java Platform. Run following commands as root or user with sudo access to install OpenJDK package.

sudo dnf install java-11-openjdk-devel

Step 2: Create tomcat user and group

It’s a security risk to to run Tomcat under root user. We need to create a new user and group dedicated to running tomcat service. To do so, run the following command:

sudo useradd -m -U -d /opt/tomcat -s /bin/false tomcat

Step 3: Install Tomcat 9 on Linux CentOS 8 (Or Ubuntu 20.04)

It’s always best practice to Check the latest release version of Tomcat 9. At the time of writing this tutorial, the latest Tomcat version 9.0.44. Save the version number to VERSION variable and proceed to download.

After that, navigate to the /tmp directory and download the latest Tomcat binary release:

cd /tmp

VERSION=9.0.44

wget https://www-eu.apache.org/dist/tomcat/tomcat-9/v${VERSION}/bin/apache-tomcat-${VERSION}.tar.gz

Once download complete extract archive and move to /opt/tomcat directory.

sudo tar -xf /tmp/apache-tomcat-${VERSION}.tar.gz -C /opt/tomcat/

Now, Create a symbolic link with name latest that points to the Tomcat installation directory. Later when upgrading Tomcat, you can easily migrate to another Tomcat version just by changing the symlink to point to the desired version.

sudo ln -s /opt/tomcat/apache-tomcat-${VERSION} /opt/tomcat/latest

Step 4: Set Permissions

Previously created user must have ownership of the /opt/tomcat directory. Set proper directory permissions by running the below command:

sudo chown -R tomcat: /opt/tomcat

Create a script inside the bin directory executable:

sudo sh -c 'chmod +x /opt/tomcat/latest/bin/\*.sh'

Step 5: Create Systemd Unit File

Create a new unit file to run Tomcat as a service. Using text editor create a tomcat.service file inside /etc/systemd/system/ directory:

sudo nano /etc/systemd/system/tomcat.service

Now, add the following code into the file.

[Unit]

Description=Tomcat 9 servlet container

After=network.target

[Service]

Type=forking

User=tomcat

Group=tomcat

Environment="JAVA\_HOME=/usr/lib/jvm/jre"

Environment="JAVA\_OPTS=-Djava.security.egd=file:///dev/urandom"

Environment="CATALINA\_BASE=/opt/tomcat/latest"

Environment="CATALINA\_HOME=/opt/tomcat/latest"

Environment="CATALINA\_PID=/opt/tomcat/latest/temp/tomcat.pid"

Environment="CATALINA\_OPTS=-Xms512M -Xmx1024M -server -XX:+UseParallelGC"

ExecStart=/opt/tomcat/latest/bin/startup.sh

ExecStop=/opt/tomcat/latest/bin/shutdown.sh

[Install]

WantedBy=multi-user.target

Save and close the file.

After that reload systemd daemon to notify systemd that a new file created and start the Tomcat service:

sudo systemctl daemon-reload

Next, start and enable the Tomcat service:

sudo systemctl start tomcat

sudo systemctl enable --now tomcat

Check service status with the following command:

sudo systemctl status tomcat

● tomcat.service - Tomcat 9 servlet container

Loaded: loaded (/etc/systemd/system/tomcat.service; enabled; vendor preset: disabled)

Active: active (running) since Wed 2020-04-15 20:38:07 UTC; 28s ago

Process: 32520 ExecStart=/opt/tomcat/latest/bin/startup.sh (code=exited, status=0/SUCCESS)

Main PID: 31028 (java)

Step 6: Configure Firewall

If your server is protected by Firewall and you need to access tomcat outside of local network then you should open port 8080.

sudo firewall-cmd --permanent --zone=public --add-port=8080/tcp

sudo firewall-cmd --reload

Step 7: Configuring Tomcat Web Management Interface

At this point, Tomcat is installed and time to create user and roles to access web interface. The tomcat-users.xml file contains Tomcat users and their roles. Edit tomcat-users.xml configuration file by running following command:

sudo nano /opt/tomcat/latest/conf/tomcat-users.xml

We will define a new user in this file to access tomcat manager-gui and admin-gui. Its strongly recommended to set strong password for users.

<tomcat-users>

<!--

`    `Comments

-->

`   `<role rolename="admin-gui"/>

`   `<role rolename="manager-gui"/>

`   `<user username="admin" password="admin\_password" roles="admin-gui,manager-gui"/>

</tomcat-users>

Save and close the above file.

By default the Tomcat web management interface does not allow access the web interface from a remote IP. It’s a security risk to allow access from a remote IP or from anywhere. If you need to access the web interface from anywhere open the following files and make file content as given below.

Open Manager app context file using below command:

sudo nano /opt/tomcat/latest/webapps/manager/META-INF/context.xml

<Context antiResourceLocking="false" privileged="true" >

<!--

`  `<Valve className="org.apache.catalina.valves.RemoteAddrValve"

`         `allow="127\.\d+\.\d+\.\d+|::1|0:0:0:0:0:0:0:1" />

-->

</Context>

Run below command to open Host Manager app context file:

sudo nano /opt/tomcat/latest/webapps/host-manager/META-INF/context.xml

<Context antiResourceLocking="false" privileged="true" >

<!--

`  `<Valve className="org.apache.catalina.valves.RemoteAddrValve"

`         `allow="127\.\d+\.\d+\.\d+|::1|0:0:0:0:0:0:0:1" />

-->

</Context>

Save and close the files and restart the Tomcat server, type:

sudo systemctl restart tomcat

It is also allowed to set a specific IP to access web interface instead of from anywhere. Do not comment the blocks add your public IP to the list. For exmaple, your public IP is 51.21.36.102 then it should look like below:

<Context antiResourceLocking="false" privileged="true" >

`  `<Valve className="org.apache.catalina.valves.RemoteAddrValve"

`         `allow="127\.\d+\.\d+\.\d+|::1|0:0:0:0:0:0:0:1|51.21.36.102" />

</Context>

<Context antiResourceLocking="false" privileged="true" >

`  `<Valve className="org.apache.catalina.valves.RemoteAddrValve"

`         `allow="127\.\d+\.\d+\.\d+|::1|0:0:0:0:0:0:0:1|51.21.36.102" />

</Context>

You can add more IP address with vertical bar separator. Again, Restart the Tomcat service for changes to take effect:

sudo systemctl restart tomcat

Step 9: Access Tomcat Web interface

Open your favorite web browser and type: http://your\_domain\_or\_IP\_address:8080

It should appear page as given below if your installation is successful.
# Create a bootable USB Ubuntu system disc
1. Make 1 FAT32 partition of about 100MB.
1. Make an ext4 partition using the remaining space.
1. On the installer, set the mount point of the ext4 partition to / and the mount point of the FAT32 partition to /<anything but boot>
1. Enable formatting of the partitions as part of the install (use Change if necessary)
1. Install away.
1. Create a folder called efi on the FAT32 USB partition.
1. Now the nasty bit: -
   1. Locate the boot partition of the SSD.
   1. Copy the ubuntu directory and its contents to the /efi folder created in 6.
   1. Make a copy of the ubuntu directory in /efi and rename it boot. 
   1. In the just created /efi/boot directory, rename shimx64.efi to bootx64.efi
   1. Delete the ubuntu directory on the SSD boot partition (this will prevent ubuntu options appearing on the F9 boot options menu.
1. Remove mmx64.efi* from the /efi/boot directory.
1. Remove shimx64.efi and mmx64.efi* from the /efi/ubuntu directory.

\* It may be best to keep mmx64.efi in each case because there was a problem booting in which shimx86.efi was not found after an attempted install of a system library which required a temporary change to secure boot.

