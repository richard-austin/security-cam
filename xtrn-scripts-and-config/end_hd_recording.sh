#!/bin/bash

log_file=/home/security-cam/motion-log/motionevents-$(date +%Y%m%d).log
IFS=':' read -ra FIELDS <<< "$2"

stop
  PID=$(< /home/security-cam/recording-pids/"${FIELDS[1]}-${FIELDS[2]}".pid)
  kill -INT "$PID"
  echo "$(date +%d-%m-%Y" "%T): Stopped recording, pid $PID" >> "$log_file"

stop

/etc/security-cam/check_ip_not_changed.sh
