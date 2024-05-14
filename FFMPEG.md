### ffmpeg
ffmpeg version 4 is used in the NVR as versions 5 and above will not correctly mux the rtsp to fmp4 when audio is present.
The problem is due to a lack of timestamps in one or both of the streams, and whereas ffmpeg4
deals with it satisfactorily, version 5 and above don't want to know, giving the below error messages continuously, as well
as very poor video results in the application.
<pre>
pts has no value
Packet duration: -1131 / dts: 152727 is out of range
</pre>

As ffmpeg 4 is not available as standard on Ubuntu 24.04, I have built an ffmpeg v4 executable
for ARM64 architecture (in the release area of [THIS](https://github.com/richard-austin/ffmpeg-4.4.4) repository) 
which is automatically deployed by the .deb installer.


