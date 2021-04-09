#!/bin/bash
# set -x
DATE=`date +%d-%m-%Y-%T`
sed -i -e "/securityCamVersion\:/s/\:.*/: '`git describe --tags --dirty`'/" grails-app/conf/application.yml
