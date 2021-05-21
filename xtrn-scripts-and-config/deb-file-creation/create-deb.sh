#!/bin/bash


export VERSION=2.0.1

rm -r security-cam_${VERSION}_arm64

mkdir -p security-cam_${VERSION}_arm64/etc/security-cam
mkdir security-cam_${VERSION}_arm64/tmp
cp ../log_movement.sh ../end_log_movement.sh ../processmotionrecordings.sh ../processrecordings.sh ../porch_cam_mask.pgm ../garage_cam_mask.pgm security-cam_${VERSION}_arm64/etc/security-cam
mkdir -p security-cam_${VERSION}_arm64/DEBIAN
cp postinst prerm security-cam_${VERSION}_arm64/DEBIAN
cp apache-tomcat-9.0.46/apache-tomcat-9.0.46.tar.gz apache-tomcat-9.0.46/conf/server.xml apache-tomcat-9.0.46/tomcat-service security-cam_${VERSION}_arm64/tmp
cp nginx/nginx-1.21.0.tar nginx/nginx.service security-cam_${VERSION}_arm64/tmp

#touch security-cam_${VERSION}_arm64/DEBIAN/control
cat << EOF > security-cam_${VERSION}_arm64/DEBIAN/control
Package: security-cam
Version: $VERSION
Architecture: arm64
Maintainer: Richard Austin <richard.david.austin@gmail.com>
Description: A security camera system accessed through a secure web based interface.
Depends: openjdk-11-jre-headless (>=11.0.11), openjdk-11-jre-headless (<< 12.0.0),
 ffmpeg (>=7:4.2.4), ffmpeg (<<7:5.0.0),
 motion (>=4.3.2-1), motion(<<5.0.0-0)
EOF

dpkg-deb --build --root-owner-group security-cam_${VERSION}_arm64
