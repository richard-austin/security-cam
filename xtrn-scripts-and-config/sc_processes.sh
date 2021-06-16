#!/bin/bash

log_dir=/home/security-cam/logs/

run_ffmpeg() {
  while true; do
    ffmpeg -hide_banner -loglevel error -stimeout 1000000 -re -rtsp_transport tcp -i "$1" -an -c copy -f flv rtmp://localhost/"$2/$3" 2>> ${log_dir}ffmpeg_"$2"_"$3"_"$(date +%Y%m%d)".log
    sleep 1
    # ffmpeg -hide_banner -loglevel error -stimeout 1000000 -re -rtsp_transport tcp -i $1 -c copy -c:a aac -b:a 160k -ar 44100 -f flv rtmp://localhost/$2/$3 2>> ${log_dir}ffmpeg_$2_$3_`date +%Y%m%d`.log
    echo "ffmpeg terminated at $(date +%d-%m-%Y" "%T)" >> "${log_dir}ffmpeg_$2_$3_$(date +%Y%m%d)".log
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

wait
