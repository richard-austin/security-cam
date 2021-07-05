#!/bin/bash

read -r last_ip </home/security-cam/myip

echo "Last IP = ${last_ip}"

current_ip=$(curl -s 'https://api.ipify.org/?format=json' | python3 -c "import sys, json; print(json.load(sys.stdin)['ip'])")
echo "Current IP = ${current_ip}"

# The myip file is updated when the user uses the Save Current IP option in the web application
#  in response to the email sent here

if [ "$current_ip" != "$last_ip" ]; then
  ## Send the email with the ssmtp command
  ssmtp richard.david.austin@gmail.com <<EOT
From: "Raspberry pi" <rdaustin@virginmedia.com>
Subject: Change of public IP address

Hi Richard,

I have detected a change of Virgin Media broadband IP address, this is now https://${current_ip}

Please go to the web application at the new address and use the "Save Current Public IP" option  on the General menu to stop these emails continuing to be sent.

Thanks

Raspberry pi
EOT
fi
