#!/bin/bash
# set -x

mkdir -p grails-app/assets/version
git describe --tags --dirty | tr -d '\n' > grails-app/assets/version/version.txt
