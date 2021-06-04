#!/bin/bash

log_file=/home/www-data/motion-log/motionevents-$(date +%Y%m%d).log
IFS=':' read -ra FIELDS <<< "$2"

start () {
	ffmpeg -re -i rtmp://localhost:1935/"${FIELDS[1]}/${FIELDS[2]}" -t 01:00:00 -an -c copy -level 3.0 -start_number 0 -hls_time 3 -hls_list_size 0 -f hls /home/www-data/"${FIELDS[3]}/${FIELDS[1]}-$(date "+%s")_.m3u8" &
	echo $! > /home/www-data/recording-pids/"${FIELDS[1]}-${FIELDS[2]}".pid
	echo "$(date +%d-%M-%Y" "%T): Started recording- ffmpeg -re -i rtmp://localhost:1935/${FIELDS[1]}/${FIELDS[2]}" -an -c copy -level 3.0 -start_number 0 -hls_time 3 -hls_list_size 0 -f hls /home/www-data/"${FIELDS[3]}/${FIELDS[1]}-$(date "+%s")_.m3u8" >> "${log_file}"

  # Remove oldest recording files (older than 2 weeks)
  find /home/www-data/"${FIELDS[3]}" -mmin +20160 -exec rm {} \;
}

start
