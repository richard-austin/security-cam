#!/bin/bash

log_file=/home/www-data/motion-log/motionevents-`date +%Y%m%d`.log
IFS=':' read -ra FIELDS <<< $2

start () {
	echo "`date +%d-%M-%Y" "%T`: curl \"http://localhost:8083/control/record/start?app=${FIELDS[1]}&name=${FIELDS[2]}&rec=${FIELDS[3]}\"" >> $log_file
	result=`curl "http://localhost:8083/control/record/start?app=${FIELDS[1]}&name=${FIELDS[2]}&rec=${FIELDS[3]}"`
	echo -e $result >> $log_file
}

start
