#!/bin/bash

log_file=/home/security-cam/motion-log/motionevents-$(date +%Y%m%d).log

if [[ -f /home/security-cam/cameras_dev.json ]]; then
  URI=$(python3 -c "import sys, json; print(json.load(sys.stdin)['$1']['nms-uri'])" < /home/security-cam/cameras_dev.json)
  LOCATION=$(python3 -c "import sys, json; print(json.load(sys.stdin)['$1']['recording']['location'])" < /home/security-cam/cameras_dev.json)
else
  URI=$(python3 -c "import sys, json; print(json.load(sys.stdin)['$1']['nms-uri'])" < /home/security-cam/cameras.json)
  LOCATION=$(python3 -c "import sys, json; print(json.load(sys.stdin)['$1']['recording']['location'])" < /home/security-cam/cameras.json)
fi

start () {
	ffmpeg -i "$URI" -t 01:00:00 -an -c copy -level 3.0 -start_number 0 -hls_time 3 -hls_list_size 0 -f hls /home/security-cam/"$LOCATION/$LOCATION-$(date "+%s")_.m3u8" &
	echo $! > /home/security-cam/recording-pids/"$LOCATION".pid
	echo "$(date +%d-%m-%Y" "%T): Started recording: PID $(cat /home/security-cam/recording-pids/"$LOCATION".pid): - ffmpeg -i $URI -t 01:00:00 -an -c copy -level 3.0 -start_number 0 -hls_time 3 -hls_list_size 0 -f hls /home/security-cam/$LOCATION/$LOCATION-$(date "+%s")_.m3u8" >> "${log_file}"

  # Remove oldest recording files (older than 2 weeks)
  find /home/security-cam/"$LOCATION" -mmin +3
}

start

wait

