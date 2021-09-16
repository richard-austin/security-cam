#!/bin/bash

export VERSION
VERSION=$(< ../../server/grails-app/assets/version/version.txt)

rm -r security-cam_*_arm64

mkdir -p security-cam_"${VERSION}"_arm64/etc/security-cam

cp ../start_hd_recording.sh ../end_hd_recording.sh ../processmotionrecordings.sh \
 ../porch_cam_mask.pgm ../garage_cam_mask.pgm ../sc_processes.sh \
 security-cam_"${VERSION}"_arm64/etc/security-cam

tar -xf nms.tar --directory security-cam_"${VERSION}"_arm64/etc/security-cam

mkdir -p security-cam_"${VERSION}"_arm64/DEBIAN
cp preinst postinst prerm postrm security-cam_"${VERSION}"_arm64/DEBIAN

mkdir -p security-cam_"${VERSION}"_arm64/home/security-cam/hls
mkdir security-cam_"${VERSION}"_arm64/home/security-cam/recording-pids
mkdir security-cam_"${VERSION}"_arm64/home/security-cam/hls2
mkdir security-cam_"${VERSION}"_arm64/home/security-cam/hls3
mkdir security-cam_"${VERSION}"_arm64/home/security-cam/logs
mkdir security-cam_"${VERSION}"_arm64/home/security-cam/motion-hls2lo
mkdir security-cam_"${VERSION}"_arm64/home/security-cam/motion-hls3lo
mkdir security-cam_"${VERSION}"_arm64/home/security-cam/motion-hlslo
mkdir security-cam_"${VERSION}"_arm64/home/security-cam/motion
mkdir security-cam_"${VERSION}"_arm64/home/security-cam/motion-log
mkdir -p security-cam_"${VERSION}"_arm64/var/log/motion
cp ../cameras.json security-cam_"${VERSION}"_arm64/home/security-cam

mkdir -p security-cam_"${VERSION}"_arm64/tmp

mkdir -p security-cam_"${VERSION}"_arm64/lib/systemd/system/

cp -r ../motion/motion.conf ../motion/conf.d ../nginx.conf ../chrony.conf ../ssmtp.conf security-cam_"${VERSION}"_arm64/tmp
cp ../apache-tomcat-9.0.46/conf/server.xml ../apache-tomcat-9.0.46/conf/tomcat-users.xml security-cam_"${VERSION}"_arm64/tmp
cp ../install-cert.sh security-cam_"${VERSION}"_arm64/tmp
cp ../../server/build/libs/server-0.1.war security-cam_"${VERSION}"_arm64/tmp
cp ../sc_processes.service security-cam_"${VERSION}"_arm64/lib/systemd/system

cat << EOF > security-cam_"${VERSION}"_arm64/DEBIAN/control
Package: security-cam
Version: $VERSION
Architecture: arm64
Maintainer: Richard Austin <richard.david.austin@gmail.com>
Description: A security camera system accessed through a secure web based interface.
Depends: openjdk-11-jre-headless (>=11.0.11), openjdk-11-jre-headless (<< 12.0.0),
 ffmpeg (>=7:4.2.4), ffmpeg (<<7:5.0.0),
 motion (>=4.3.2-1), motion(<<5.0.0-0),
 nginx (>=1.18.0), nginx(<=1.20.9),
 tomcat9 (>=9.0.43-1), tomcat9 (<= 10.0.0),
 tomcat9-admin (>=9.0.43-1), tomcat9-admin (<= 10.0.0),
 libraspberrypi-bin, chrony,
 nodejs, ssmtp
EOF

dpkg-deb --build --root-owner-group security-cam_"${VERSION}"_arm64
