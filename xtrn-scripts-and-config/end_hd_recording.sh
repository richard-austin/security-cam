#!/bin/bash

log_file=/home/security-cam/motion-log/motionevents-$(date +%Y%m%d).log
LOCATION=$(python3 -c "import sys, json; print(json.load(sys.stdin)['camera1']['recording']['location'])" < /home/security-cam/cameras.json)

stop() {
  PID=$(</home/security-cam/recording-pids/"$LOCATION".pid)
  kill -INT "$PID"
  echo "$(date +%d-%m-%Y" "%T): Stopped recording, pid $PID" >>"$log_file"
}

stop
