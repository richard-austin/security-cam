#!/bin/bash

log_dir=/home/security-cam/logs/
read_ip() {
    read -r last_ip </home/security-cam/myip
}

run_check_ip_not_changed() {
  while true; do
    sleep 15m
    read_ip

    echo "Last IP = ${last_ip}"

    current_ip=$(curl -s 'https://api.ipify.org/?format=json' | python3 -c "import sys, json; print(json.load(sys.stdin)['ip'])")
    echo "Current IP = ${current_ip}"

    # The myip file is updated when the user uses the Save Current IP option in the web application
    #  in response to the email sent here

    while [ "$current_ip" != "$last_ip" ]; do
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
      sleep 60m
      read_ip
    done
  done
}

run_ffmpeg() {
  while true; do
    ffmpeg -hide_banner -loglevel error -stimeout 1000000 -rtsp_transport tcp -i "$1" -an -c copy -f flv rtmp://localhost/"$2/$3" 2>>${log_dir}ffmpeg_"$2"_"$3"_"$(date +%Y%m%d)".log
    sleep 1
    # ffmpeg -hide_banner -loglevel error -stimeout 1000000 -re -rtsp_transport tcp -i $1 -c copy -c:a aac -b:a 160k -ar 44100 -f flv rtmp://localhost/$2/$3 2>> ${log_dir}ffmpeg_$2_$3_`date +%Y%m%d`.log
    echo "ffmpeg terminated at $(date +%d-%m-%Y" "%T)" >>"${log_dir}ffmpeg_$2_$3_$(date +%Y%m%d)".log
  done
}

run_nms() {
  while true; do
    node /etc/security-cam/nms/app.js
    sleep 1
  done
}

run_motion() {
  while true; do
    motion
    sleep 1
  done
}

run_nms &
run_motion &
run_ffmpeg rtsp://192.168.0.30:554/11 live porch &
run_ffmpeg rtsp://192.168.0.30:554/12 livelo porch &
run_ffmpeg rtsp://192.168.0.34:554/11 live2 cam2 &
run_ffmpeg rtsp://192.168.0.34:554/12 live2lo cam2 &
run_ffmpeg rtsp://192.168.0.35:554/11 live3 cam3 &
run_ffmpeg rtsp://192.168.0.35:554/12 live3lo cam3 &
run_check_ip_not_changed &

wait
