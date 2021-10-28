#!/bin/bash

log_dir=/home/security-cam/logs/
ipV4RegEx="^([0-9]|[1-9][0-9]|1([0-9][0-9])|2([0-4][0-9]|5[0-5]))\.([0-9]|[1-9][0-9]|1([0-9][0-9])|2([0-4][0-9]|5[0-5]))\.([0-9]|[1-9][0-9]|1([0-9][0-9])|2([0-4][0-9]|5[0-5]))\.([0-9]|[1-9][0-9]|1([0-9][0-9])|2([0-4][0-9]|5[0-5]))$"

read_ip() {
    read -r last_ip </home/security-cam/myip

    current_reading=$(curl -s 'https://api.ipify.org/?format=json' | python3 -c "import sys, json; print(json.load(sys.stdin)['ip'])")

    if [[ $current_reading =~ $ipV4RegEx  ]]; then
      current_ip=${current_reading}
    else
      echo "$(date +%d-%m-%Y" "%T): Bad reading (${current_reading}) from https://api.ipify.org" >>"${log_dir}ipify_$(date +%Y%m%d)".log
    fi

    # The myip file is updated when the user uses the Save Current IP option in the web application
    #  in response to the email sent here
}

kill_descendant_processes() {
    local pid="$1"
    local and_self="${2: -false}"
    if children="$(pgrep -P "$pid")"; then
        for child in $children; do
            kill_descendant_processes "$child" true
        done
    fi
    if [[ "$and_self" == true ]]; then
        kill -TERM "$pid"
    fi
}

run_check_ip_not_changed() {
  while true; do
    sleep 15m
    read_ip

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
    /usr/bin/ffmpeg -hide_banner -loglevel error -stimeout 1000000 -rtsp_transport tcp -i "$1" -an -c copy -f flv "$2" 2>>"${log_dir}ffmpeg_"$3"_$(date +%Y%m%d)".log
    sleep 1
    echo "ffmpeg terminated at $(date +%d-%m-%Y" "%T)" >>"${log_dir}ffmpeg_"$3"_$(date +%Y%m%d)".log
  done
}

run_nms() {
  while true; do
    /usr/bin/node /etc/security-cam/nms/app.js
    sleep 1
  done
}

run_motion() {
  while true; do
    /usr/bin/motion
    sleep 1
  done
}

run_nms &
run_motion &
run_ffmpeg "rtsp://192.168.0.45:554/12" "rtmp://localhost:1935/nms/stream1" "Camera_1_Normal" &
run_ffmpeg "rtsp://192.168.0.45:554/11" "rtmp://localhost:1935/nms/stream2" "Camera_1_HD" &
run_ffmpeg "rtsp://192.168.0.45:554/11" "rtmp://localhost:1935/nms/stream3" "Camera_1_HD2" &

run_check_ip_not_changed &

trap 'kill_descendant_processes $$' INT EXIT TERM
wait
