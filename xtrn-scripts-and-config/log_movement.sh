#!/bin/bash

IFS=':' read -ra FIELDS <<< $2
curl "http://localhost:8083/control/record/start?app=${FIELDS[1]}&name=${FIELDS[2]}&rec=${FIELDS[3]}"

echo "curl \"http://localhost:8083/control/record/start?app=${FIELDS[1]}&name=${FIELDS[2]}&rec=${FIELDS[3]}\"" >> /home/www-data/motionevents.log
