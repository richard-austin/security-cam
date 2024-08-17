#!/bin/bash

export VERSION
VERSION=$(< ../../server/grails-app/assets/version/version.txt)

rm -r security-cam_*_arm64

mkdir -p security-cam_"${VERSION}"_arm64/etc/security-cam

cp ../processmotionrecordings.sh \
  ../productKeyGen/generateProductKey.jar ../productKeyGen/publicKey \
  ../productKeyGen/productKeyGen.sh \
 security-cam_"${VERSION}"_arm64/etc/security-cam

mkdir -p security-cam_"${VERSION}"_arm64/etc/security-cam/wifi-setup-service &&
cp ../../wifi-setup-service/*.py security-cam_"${VERSION}"_arm64/etc/security-cam/wifi-setup-service
cp ../../wifi-setup-service/requirements.txt security-cam_"${VERSION}"_arm64/etc/security-cam/wifi-setup-service
cp ../install-cert.sh security-cam_"${VERSION}"_arm64/etc/security-cam

mkdir -p security-cam_"${VERSION}"_arm64/etc/fmp4-ws-media-server &&
cp  ../fmp4-ws-media-server/fmp4-ws-media-server_arm64 security-cam_"${VERSION}"_arm64/etc/fmp4-ws-media-server
cp  ../fmp4-ws-media-server/config.json security-cam_"${VERSION}"_arm64/etc/fmp4-ws-media-server

mkdir -p security-cam_"${VERSION}"_arm64/etc/security-cam/camera-recordings-service &&
cp  ../../camera-recordings-service/*.py security-cam_"${VERSION}"_arm64/etc/security-cam/camera-recordings-service
cp  ../../camera-recordings-service/requirements.txt security-cam_"${VERSION}"_arm64/etc/security-cam/camera-recordings-service

mkdir -p security-cam_"${VERSION}"_arm64/lib/systemd/system
cp ../wifimanagement.service  security-cam_"${VERSION}"_arm64/lib/systemd/system
cp ../camera-recordings.service security-cam_"${VERSION}"_arm64/lib/systemd/system
cp ../fmp4-ws-media-server.service security-cam_"${VERSION}"_arm64/lib/systemd/system

mkdir -p security-cam_"${VERSION}"_arm64/DEBIAN
cp preinst postinst prerm postrm security-cam_"${VERSION}"_arm64/DEBIAN

mkdir -p security-cam_"${VERSION}"_arm64/var/security-cam/recording-pids
mkdir -p security-cam_"${VERSION}"_arm64/var/security-cam/motion/conf.d

mkdir -p security-cam_"${VERSION}"_arm64/var/security-cam/ftp

# Create recording file location for up top 50 streams
for ((i = 1 ; i < 51 ; i++ )); do mkdir -p security-cam_"${VERSION}"_arm64/var/security-cam/rec"$i"; done

mkdir -p security-cam_"${VERSION}"_arm64/var/log/security-cam
mkdir -p security-cam_"${VERSION}"_arm64/var/log/camera-recordings-service
mkdir -p security-cam_"${VERSION}"_arm64/var/log/fmp4-ws-media-service

mkdir -p security-cam_"${VERSION}"_arm64/var/log/motion
mkdir -p security-cam_"${VERSION}"_arm64/var/log/wifimgr
mkdir -p security-cam_"${VERSION}"_arm64/var/log/tomcat9
mkdir -p security-cam_"${VERSION}"_arm64/var/lib/tomcat9

mkdir -p security-cam_"${VERSION}"_arm64/tmp

mkdir -p security-cam_"${VERSION}"_arm64/lib/systemd/system/

cp -r ../motion/motion.conf ../nginx.conf ../chrony.conf security-cam_"${VERSION}"_arm64/tmp
cp ../apache-tomcat-9/conf/server.xml ../apache-tomcat-9/conf/tomcat-users.xml security-cam_"${VERSION}"_arm64/tmp
tar -xzf ../apache-tomcat-9/apache-tomcat-9.0.89.tar.gz -C security-cam_"${VERSION}"_arm64/var/lib/tomcat9 --strip-components=1
rmdir security-cam_"${VERSION}"_arm64/var/lib/tomcat9/logs
ln -s /var/log/tomcat9 security-cam_"${VERSION}"_arm64/var/lib/tomcat9/logs
cp ../tomcat9 security-cam_"${VERSION}"_arm64/tmp
cp ../../server/build/libs/server-7.3.war security-cam_"${VERSION}"_arm64/tmp
cp ../../initialAdmin/dist/cua.war  security-cam_"${VERSION}"_arm64/tmp
cp ../apache-tomcat-9/tomcat9.service security-cam_"${VERSION}"_arm64/lib/systemd/system/
cat << EOF > security-cam_"${VERSION}"_arm64/DEBIAN/control
Package: security-cam
Version: $VERSION
Architecture: arm64
Maintainer: Richard Austin <richard.david.austin@gmail.com>
Description: A security camera system accessed through a secure web based interface.
Depends: openjdk-21-jre-headless (>=21.0.0), openjdk-21-jre-headless (<< 21.9.9),
 ffmpeg (>=7:6.1.1), ffmpeg (<<7:6.9.9),
 motion (>=4.6), motion(<<5.0.0-0),
 curl (>=8.5.0), curl(<=8.9),
 nginx (>=1.24.0), nginx(<=1.24.9),
 libraspberrypi-bin, chrony,
 network-manager (>= 1.46.0), network-manager (<< 2.0.0),
 wireless-tools (>=30~pre9-13), wireless-tools (<< 40),
 moreutils,
 python3-pip, python3.12-venv
EOF

dpkg-deb --build --root-owner-group security-cam_"${VERSION}"_arm64
