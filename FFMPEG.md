### ffmpeg
ffmpeg version 4 is used in the NVR as versions 5 and above will not correctly mux the rtsp to fmp4 when audio is present.
The problem is due to a lack of timestamps in one or both of the streams, and whereas ffmpeg4
deals with it satisfactorily, version 5 and above don't want to know, giving the below error messages continuously.
<pre>
pts has no value
Packet duration: -1131 / dts: 152727 is out of range
</pre>

As ffmpeg 4 is not available as standard on Ubuntu 23.10, I have built an ffmpeg v4 executable
for ARM64 architecture (at xtrn-scripts-and-config/ffmpeg-v4.4.4) which is deployed on installation of the
main project deb file.

If you are deploying to a Raspberry pi, you can skip to the *Camera Recordings Service* section.

If you are deploying the NVR to a non ARM64 platform, you will need to
build ffmpeg 4 on that platform as follows:-.


```bash
wget https://ffmpeg.org/releases/ffmpeg-4.4.4.tar.xz
tar -xvf ffmpeg-4.4.4.tar.xz
cd ffmpeg-4.4.4/
./configure
make
```
The executable ffmpeg will be in the ffmpeg-4.4.4 directory. To deploy it in the deb file with the rest of the
NVR, copy it to the project at xtrn-scripts-and-config/ffmpeg-v4.4.4.
##### ffmpeg Build problem
On Ubuntu 23.10, I got the following compilation error when building ffmpeg 4: -
<pre>
CC      libavformat/adtsenc.o
./libavcodec/x86/mathops.h: Assembler messages:
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error:ffmpeg-4.4.4 operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
./libavcodec/x86/mathops.h:125: Error: operand type mismatch for `shr'
make: *** [ffbuild/common.mak:81: libavformat/adtsenc.o] Error 1
</pre>

To fix this, copy xtrn-scripts-and-config/ffmpeg-v4.4.4/mathopts.patch to
your ffmpeg-4.4.4 build directory and (from the ffmpeg-4.4.4 build directory) run

```text
patch -u -b  ./libavcodec/x86/mathops.h mathopts.patch 
```
Run
```text
make clean
make
```

and it should now build OK.

