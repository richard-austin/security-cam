#!/bin/bash

log_file=/home/www-data/motion-log/motionevents-$(date +%Y%m%d).log
IFS=':' read -ra FIELDS <<< "$2"

stop ()
{
  PID=$(< /home/www-data/recording-pids/"${FIELDS[1]}-${FIELDS[2]}".pid)
  kill -INT "$PID"
  echo "$(date +%d-%m-%Y" "%T): Stopped recording, pid $PID" >> "$log_file"
}

stop

/home/www-data/check_ip_not_changed.sh
