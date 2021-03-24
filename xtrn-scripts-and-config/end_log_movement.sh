#!/bin/bash

log_file=/home/www-data/motion-log/motionevents-`date +%Y%m%d`.log

IFS=':' read -ra FIELDS <<< $2
echo "`date`: curl \"http://localhost:8083/control/record/stop?app=${FIELDS[1]}&name=${FIELDS[2]}&rec=${FIELDS[3]}\"" >> $log_file
curl "http://localhost:8083/control/record/stop?app=${FIELDS[1]}&name=${FIELDS[2]}&rec=${FIELDS[3]}" >> $log_file
echo -e " " >> $log_file 
