package com.securitycam.recording

import org.bytedeco.ffmpeg.avcodec.AVPacket


import java.util.concurrent.BlockingQueue

import static com.securitycam.recording.deepCopy.freePacket
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free

class PacketQueue {
    private int index
    private ArrayList<byte[]> cacheSnapshot
    int cacheLength
    boolean stopped

    BlockingQueue q

    PacketQueue(GOPCache src) {
        stopped = false
        this.cacheLength = src.cacheLength
        this.cacheSnapshot = new ArrayList<AVPacket>(src.cacheLength)
        this.index = 0
        this.q = src.getQueue()
    }

    AVPacket getNextPacket() {
        return q.take() as AVPacket
    }

    void putPacket(AVPacket pkt) {
        if(!stopped)
            q.put(pkt)
    }

    void stop() {
        stopped = true
        while(!q.empty) {
            def pkt = q.take() as AVPacket
            freePacket(pkt)
        }
    }
}
