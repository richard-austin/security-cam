#!/bin/bash
set -x
DATE=`date +%d-%m-%Y-%T`
sed -i -e "/version\:/s/\:.*/: '`git describe --tags` `echo $DATE`'/" grails-app/conf/application.yml
