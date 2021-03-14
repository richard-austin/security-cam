#!/bin/bash

IFS=':' read -ra FIELDS <<< $2
curl "http://localhost:8083/control/record/start?app=${FIELDS[1]}&name=${FIELDS[2]}&rec=${FIELDS[3]}"
curl "http://localhost:8083/control/record/start?app=${FIELDS[4]}&name=${FIELDS[5]}&rec=${FIELDS[6]}"

echo "curl http://localhost:8083/control/record/start?app=${FIELDS[1]}&name=${FIELDS[2]}&rec=${FIELDS[3]}" >> /home/www-data/motionevents.log

echo "curl http://localhost:8083/control/record/start?app=${FIELDS[4]}&name=${FIELDS[5]}&rec=${FIELDS[6]}" >> /home/www-data/motionevents.log

#touch /home/www-data/motion-log/$2-$1-moved-at-`date +%s`
