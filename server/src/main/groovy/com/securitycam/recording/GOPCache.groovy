package com.securitycam.recording

import com.securitycam.services.LogService
import org.bytedeco.ffmpeg.avcodec.AVPacket

class GOPCache {
    LogService logService
    AVPacket[] packets
    final int length

    int packetIndex

    GOPCache(int length, LogService logService) {
        this.logService = logService
        this.length = length
        packets = new AVPacket[length]
        packetIndex = 0
    }

    void write(AVPacket pkt) {
        if(packetIndex < length)
            packets[packetIndex++] = pkt
        else
            logService.cam.error("GOPCache length exceeded ("+length+")")
    }

    void writeKeyFrame(AVPacket pkt) {
        packetIndex = 0
        write(pkt)
    }
}
