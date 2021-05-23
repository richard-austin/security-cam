#!/bin/bash


export VERSION=2.0.1

rm -r security-cam_${VERSION}_arm64

mkdir -p security-cam_${VERSION}_arm64/etc/security-cam
cp ../log_movement.sh ../end_log_movement.sh ../processmotionrecordings.sh \
 ../processrecordings.sh ../porch_cam_mask.pgm ../garage_cam_mask.pgm ../ffmpeg_with_logging.sh \
 security-cam_${VERSION}_arm64/etc/security-cam

mkdir -p security-cam_${VERSION}_arm64/DEBIAN
cp postinst prerm security-cam_${VERSION}_arm64/DEBIAN

mkdir -p security-cam_${VERSION}_arm64/home/www-data/hls
mkdir security-cam_${VERSION}_arm64/home/www-data/hls2
mkdir security-cam_${VERSION}_arm64/home/www-data/hls3
mkdir -p security-cam_${VERSION}_arm64/home/www-data/live/hls
mkdir security-cam_${VERSION}_arm64/home/www-data/live/hls2
mkdir security-cam_${VERSION}_arm64/home/www-data/live/hls2lo
mkdir security-cam_${VERSION}_arm64/home/www-data/live/hls3
mkdir security-cam_${VERSION}_arm64/home/www-data/live/hls3lo
mkdir security-cam_${VERSION}_arm64/home/www-data/live/hlslo
mkdir security-cam_${VERSION}_arm64/home/www-data/logs
mkdir security-cam_${VERSION}_arm64/home/www-data/motion-hls2lo
mkdir security-cam_${VERSION}_arm64/home/www-data/motion-hls3lo
mkdir security-cam_${VERSION}_arm64/home/www-data/motion-hlslo
mkdir security-cam_${VERSION}_arm64/home/www-data/motion-log
mkdir -p security-cam_${VERSION}_arm64/var/log/motion

mkdir -p security-cam_${VERSION}_arm64/tmp

cp -r ../motion.conf ../conf.d security-cam_${VERSION}_arm64/tmp
cp ../apache-tomcat-9.0.46/conf/server.xml security-cam_${VERSION}_arm64/tmp

cat << EOF > security-cam_${VERSION}_arm64/DEBIAN/control
Package: security-cam
Version: $VERSION
Architecture: arm64
Maintainer: Richard Austin <richard.david.austin@gmail.com>
Description: A security camera system accessed through a secure web based interface.
Depends: openjdk-11-jre-headless (>=11.0.11), openjdk-11-jre-headless (<< 12.0.0),
 ffmpeg (>=7:4.2.4), ffmpeg (<<7:5.0.0),
 motion (>=4.3.2-1), motion(<<5.0.0-0),
 nginx (>=1.18.0), nginx(<=1.20.9),
 libnginx-mod-rtmp (>=1.18.0), libnginx-mod-rtmp (<=1.20.9),
 tomcat9 (>=9.0.43-1), tomcat9 (<= 10.0.0)
EOF

dpkg-deb --build --root-owner-group security-cam_${VERSION}_arm64
