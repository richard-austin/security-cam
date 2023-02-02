#!/bin/bash

log_file=/var/log/motion/motionevents-$(date +%Y%m%d).log
IFS='.';read -ra FIELDS <<< "$1"

if [[ -f /var/security-cam/cameras_dev.json ]]; then
  URI=$(python3 -c "import sys, json; print(json.load(sys.stdin)['${FIELDS[0]}']['streams']['${FIELDS[1]}']['media_server_input_uri'])" < /var/security-cam/cameras_dev.json)
  LOCATION=$(python3 -c "import sys, json; print(json.load(sys.stdin)['${FIELDS[0]}']['streams']['${FIELDS[1]}']['recording']['location'])" < /var/security-cam/cameras_dev.json)
else
  URI=$(python3 -c "import sys, json; print(json.load(sys.stdin)['${FIELDS[0]}']['streams']['${FIELDS[1]}']['media_server_input_uri'])" < /var/security-cam/cameras.json)
  LOCATION=$(python3 -c "import sys, json; print(json.load(sys.stdin)['${FIELDS[0]}']['streams']['${FIELDS[1]}']['recording']['location'])" < /var/security-cam/cameras.json)
fi

start () {
	ffmpeg -i "$URI" -t 01:00:00 -an -c copy -level 3.0 -start_number 0 -hls_time 3 -hls_list_size 0 -f hls /var/security-cam/"$LOCATION/$LOCATION-$(date "+%s")_.m3u8" &
	echo $! > /var/security-cam/recording-pids/"$LOCATION".pid
	echo "$(date +%d-%m-%Y" "%T): Started recording: PID $(cat /var/security-cam/recording-pids/"$LOCATION".pid): - ffmpeg -i $URI -t 01:00:00 -an -c copy -level 3.0 -start_number 0 -hls_time 3 -hls_list_size 0 -f hls /var/security-cam/$LOCATION/$LOCATION-$(date "+%s")_.m3u8" >> "${log_file}"

  # Remove oldest recording files (older than 3 weeks)
  nice -10 find /var/security-cam/"$LOCATION" -mtime +21 -delete
}

start

wait

