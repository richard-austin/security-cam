#!/bin/bash

log_file=/home/security-cam/motion-log/motionevents-$(date +%Y%m%d).log

if [[ -f /home/security-cam/cameras_dev.json ]]; then
  LOCATION=$(python3 -c "import sys, json; print(json.load(sys.stdin)['$1']['recording']['location'])" < /home/security-cam/cameras_dev.json)
else
  LOCATION=$(python3 -c "import sys, json; print(json.load(sys.stdin)['$1']['recording']['location'])" < /home/security-cam/cameras.json)
fi

stop() {
  PID=$(</home/security-cam/recording-pids/"$LOCATION".pid)
  kill -INT "$PID"
  echo "$(date +%d-%m-%Y" "%T): Stopped recording, pid $PID" >>"$log_file"
}

stop

