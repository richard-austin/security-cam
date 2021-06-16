# security-cam
CCTV Security cam project not using a cloud service. Run on Raspberry pi

## Set up the sec-cam home directory
The project requires that the nginx user (sec-cam) has a "home" directory /home/security-cam owned by sec-cam.

This directory has the following structure:-

security-cam/  
├── db  
├── hls  
├── hls2  
├── hls3  
├── logs  
├── motion-hls2lo  
├── motion-hls3lo  
├── motion-hlslo  
└── motion-log

A new group, security-cam, should be added, then the users tomcat and sec-cam added to that group. 
The security-cam group is then given group access to sec-cam and it's subdirectories. The user and group permissions are then set up.

 sudo groupadd security-cam  
 sudo usermod -a -G security-cam tomcat  
 sudo usermod -a -G security-cam sec-cam  
 sudo chmod -R 775 /home/security-cam/  
 sudo chgrp -R security-cam /home/security-cam/  

 ## The following files are placed at /etc/security-cam
+ start_hd_recording.sh 
+ end_hd_recording.sh
+ processmotionrecordings.sh
+ garage_cam_mask.pgm
+ porch_cam_mask.pgm 

*/etc/security_cam is owned by sec-cam:sec-cam*

 ## Tomcat using vcgencmd measure_temp
  To execute the getTemperature API call, Tomcat needs to be able to call vcgencmd measure_temp which requires it to be in the video group.
 
 sudo usermod -aG video tomcat
