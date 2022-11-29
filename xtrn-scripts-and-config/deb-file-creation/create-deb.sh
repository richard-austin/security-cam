#!/bin/bash

export VERSION
VERSION=$(< ../../server/grails-app/assets/version/version.txt)

rm -r security-cam_*_arm64

mkdir -p security-cam_"${VERSION}"_arm64/etc/security-cam

cp ../start_hd_recording.sh ../end_hd_recording.sh ../processmotionrecordings.sh \
  ../cacert.jks ../client.jks ../productKeyGen/generateProductKey.jar ../productKeyGen/publicKey \
  ../productKeyGen/productKeyGen.sh \
 security-cam_"${VERSION}"_arm64/etc/security-cam

mkdir -p security-cam_"${VERSION}"_arm64/etc/security-cam/wifi-setup-service &&
cp ../../wifi-setup-service/src/*.py security-cam_"${VERSION}"_arm64/etc/security-cam/wifi-setup-service
cp ../../wifi-setup-service/src/requirements.txt security-cam_"${VERSION}"_arm64/etc/security-cam/wifi-setup-service

mkdir -p security-cam_"${VERSION}"_arm64/etc/security-cam/camera-recordings-service &&
cp  ../../camera-recordings-service/src/*.py security-cam_"${VERSION}"_arm64/etc/security-cam/camera-recordings-service
cp  ../../camera-recordings-service/src/requirements.txt security-cam_"${VERSION}"_arm64/etc/security-cam/camera-recordings-service

mkdir -p security-cam_"${VERSION}"_arm64/usr/bin
mkdir -p security-cam_"${VERSION}"_arm64/etc/motion

mkdir -p security-cam_"${VERSION}"_arm64/lib/systemd/system
cp ../wifimanagement.service  security-cam_"${VERSION}"_arm64/lib/systemd/system
cp ../camera-recordings.service security-cam_"${VERSION}"_arm64/lib/systemd/system
cp ../motion/motion.service security-cam_"${VERSION}"_arm64/lib/systemd/system/motion.service
cp ../motion/motion security-cam_"${VERSION}"_arm64/usr/bin
cp ../motion/motion.conf security-cam_"${VERSION}"_arm64/etc/motion

tar -xf nms.tar --directory security-cam_"${VERSION}"_arm64/etc/security-cam

mkdir -p security-cam_"${VERSION}"_arm64/DEBIAN
cp preinst postinst prerm postrm security-cam_"${VERSION}"_arm64/DEBIAN

mkdir -p security-cam_"${VERSION}"_arm64/etc/security-cam/recording-pids
mkdir -p security-cam_"${VERSION}"_arm64/etc/security-cam/motion/conf.d

mkdir -p security-cam_"${VERSION}"_arm64/var/security-cam/ftp
mkdir security-cam_"${VERSION}"_arm64/var/security-cam/rec1
mkdir security-cam_"${VERSION}"_arm64/var/security-cam/rec2
mkdir security-cam_"${VERSION}"_arm64/var/security-cam/rec3
mkdir security-cam_"${VERSION}"_arm64/var/security-cam/rec4
mkdir security-cam_"${VERSION}"_arm64/var/security-cam/rec5
mkdir security-cam_"${VERSION}"_arm64/var/security-cam/rec6
mkdir security-cam_"${VERSION}"_arm64/var/security-cam/rec7
mkdir security-cam_"${VERSION}"_arm64/var/security-cam/rec8
mkdir security-cam_"${VERSION}"_arm64/var/security-cam/rec9
mkdir security-cam_"${VERSION}"_arm64/var/security-cam/rec10
mkdir -p security-cam_"${VERSION}"_arm64/var/log/security-cam
mkdir -p security-cam_"${VERSION}"_arm64/var/log/camera-recordings-service
mkdir -p security-cam_"${VERSION}"_arm64/var/log/motion
mkdir -p security-cam_"${VERSION}"_arm64/var/log/wifimgr
mkdir -p security-cam_"${VERSION}"_arm64/var/lib/tomcat

mkdir -p security-cam_"${VERSION}"_arm64/tmp

cp -r ../nginx.conf ../chrony.conf ../ssmtp.conf security-cam_"${VERSION}"_arm64/tmp
cp ../apache-tomcat-9.0.46/conf/server.xml ../apache-tomcat-9.0.46/conf/tomcat-users.xml security-cam_"${VERSION}"_arm64/tmp
cp ../install-cert.sh ../tomcat9 security-cam_"${VERSION}"_arm64/tmp
cp ../../server/build/libs/server-7.3.war security-cam_"${VERSION}"_arm64/tmp

cat << EOF > security-cam_"${VERSION}"_arm64/DEBIAN/control
Package: security-cam
Version: $VERSION
Architecture: arm64
Maintainer: Richard Austin <richard.david.austin@gmail.com>
Description: A security camera system accessed through a secure web based interface.
Depends: openjdk-17-jre-headless (>=17.0.3), openjdk-17-jre-headless (<< 18.0.0),
 ffmpeg (>=7:4.2.4), ffmpeg (<<7:5.0.0),
 nginx (>=1.18.0), nginx(<=1.20.9),
 tomcat9 (>=9.0.43-1), tomcat9 (<= 10.0.0),
 tomcat9-admin (>=9.0.43-1), tomcat9-admin (<= 10.0.0),
 libraspberrypi-bin, chrony,
 nodejs, ssmtp,
 network-manager (>= 1.36.4), network-manager (<< 2.0.0),
 wireless-tools (>=30~pre9-13), wireless-tools (<< 40),
 python3-pip, autoconf, automake, libtool, libavcodec-dev, libavdevice-dev, libavformat-dev, libswscale-dev, libjpeg-dev,
 libpq-dev, libsqlite3-dev, debhelper, dh-autoreconf, libwebp-dev, libmicrohttpd-dev, gettext, libmariadb-dev
EOF

dpkg-deb --build --root-owner-group security-cam_"${VERSION}"_arm64
