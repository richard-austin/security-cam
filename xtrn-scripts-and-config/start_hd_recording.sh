#!/bin/bash

log_file=/home/security-cam/motion-log/motionevents-$(date +%Y%m%d).log
IFS=':' read -ra FIELDS <<< "$2"

start () {
	ffmpeg -re -i rtmp://localhost:1935/"${FIELDS[1]}/${FIELDS[2]}" -t 01:00:00 -an -c copy -level 3.0 -start_number 0 -hls_time 3 -hls_list_size 0 -f hls /home/security-cam/"${FIELDS[3]}/${FIELDS[1]}-$(date "+%s")_.m3u8" &
	echo $! > /home/security-cam/recording-pids/"${FIELDS[1]}-${FIELDS[2]}".pid
	echo "$(date +%d-%m-%Y" "%T): Started recording: PID $(cat /home/security-cam/recording-pids/"${FIELDS[1]}-${FIELDS[2]}".pid): - ffmpeg -i rtmp://localhost:1935/${FIELDS[1]}/${FIELDS[2]}" -an -c copy -level 3.0 -start_number 0 -hls_time 3 -hls_list_size 0 -f hls /home/security-cam/"${FIELDS[3]}/${FIELDS[1]}-$(date "+%s")_.m3u8" >> "${log_file}"

  # Remove oldest recording files (older than 2 weeks)
  find /home/security-cam/"${FIELDS[3]}" -mmin +30240 -exec rm {} \;
}

start

# cat cameras.json | python3 -c "import sys, json; print(json.load(sys.stdin)['camera1']['uri'])"
wait

