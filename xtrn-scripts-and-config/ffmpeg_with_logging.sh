#!/bin/bash

log_dir=/home/www-data/logs/

ffmpeg -hide_banner -loglevel error -stimeout 1000000 -re -rtsp_transport tcp -i $1 -an -c copy -f flv rtmp://localhost/$2/$3 2>> ${log_dir}ffmpeg_$2_$3_`date +%Y%m%d`.log
# ffmpeg -hide_banner -loglevel error -stimeout 1000000 -re -rtsp_transport tcp -i $1 -c copy -c:a aac -b:a 160k -ar 44100 -f flv rtmp://localhost/$2/$3 2>> ${log_dir}ffmpeg_$2_$3_`date +%Y%m%d`.log
echo "ffmpeg terminated at `date +%d-%m-%Y" "%T`" >> ${log_dir}ffmpeg_$2_$3_`date +%Y%m%d`.log

