#!/bin/bash

# Create and install the security certificate and key for the site
openssl req -x509 -newkey rsa:4096 -keyout security-cam.key -out security-cam.crt -nodes -days 2000
chown root:root security-cam.key
chown root:root security-cam.crt
mv security-cam.key /etc/nginx
mv security-cam.crt /etc/nginx
