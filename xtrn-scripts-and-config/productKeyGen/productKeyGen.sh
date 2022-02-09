#!/bin/bash

function finish {
  # Delete everything associated with generating the product key
  rm  /etc/security-cam/publicKey
  rm /etc/security-cam/generateProductKey.jar
  rm "$0"  # Self deletion of this script
}

PROD_KEY=/etc/security-cam/prodKey
if [ ! -f "$PROD_KEY" ]; then
  cd /etc/security-cam/ || exit
  java -jar /etc/security-cam/generateProductKey.jar
fi

trap finish EXIT
