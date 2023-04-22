# security-cam
CCTV Security cam project not using a cloud service. Run on Raspberry pi

# Old ffmpeg params (included audio)
ffmpeg -stimeout 1000000 -re -rtsp_transport tcp -i rtsp://192.168.0.30:554/11 -c copy -c:a aac -b:a 160k -ar 44100 -f flv rtmp://localhost/live/porch;

# ONVIF
From https://github.com/fpompermaier/onvif

# Create the security certs
openssl req -x509 -out security-cam.crt -keyout security-cam.key -newkey rsa:2048 -nodes -sha256 -subj '/CN=localhost' -extensions EXT -config <( \
printf "[dn]\nCN=localhost\n[req]\ndistinguished_name = dn\n[EXT]\nsubjectAltName=DNS:localhost\nkeyUsage=digitalSignature\nextendedKeyUsage=serverAuth")
