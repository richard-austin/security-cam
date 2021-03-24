# security-cam
CCTV Security cam project not using a cloud service. Run on Raspberry pi

## Set up the www-data home directory
The project requires that the nginx user (www-data) has the home directory /home/www-data owned ny www-data.

This directory has the following structure:-

www-data/  
├── db  
├── hls  
├── hls2  
├── hls3  
├── live  
│   ├── hls  
│   ├── hls2  
│   ├── hls2lo  
│   ├── hls3  
│   ├── hls3lo  
│   └── hlslo  
├── logs  
├── motion-hls2lo  
├── motion-hls3lo  
├── motion-hlslo  
└── motion-log

A new group, security-cam, should be added, then the users tomcat and www-data added to that group. 
The security-cam group is then given group access to www-data and it's subdirectories. The user and group permissions are then set up.

 sudo groupadd security-cam  
 sudo usermod -a -G security-cam tomcat  
 sudo usermod -a -G security-cam www-data  
 sudo chgrp -R security-cam /home/www-data/  
 sudo chmod -R 770 /home/www-data/  
 sudo chmod -R 664 /home/www-data/  
 
