package com.securitycam.recording

import com.securitycam.services.LogService
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avformat.AVFormatContext

import java.util.concurrent.LinkedBlockingQueue

import static com.securitycam.recording.deepCopy.deepCopyPacket
import static com.securitycam.recording.deepCopy.freePacket

class GOPCache {
    LogService logService
    final AVPacket[] cache
    final int cacheLength
    int packetIndex
    boolean hasKeyFrame
    AVFormatContext formatContext

    GOPCache(int cacheLength, AVFormatContext formatContext, LogService logService) {
        this.logService = logService
        this.formatContext = formatContext
        this.cacheLength = cacheLength
        cache = new AVPacket[cacheLength]
        packetIndex = 0
        hasKeyFrame = false
    }

    void write(AVPacket pkt) {
        if (pkt == null || pkt.isNull()) return
        synchronized (cache) {
            if (hasKeyFrame && packetIndex < this.cacheLength) {
                cache[packetIndex++] = pkt
            } else {
                if (packetIndex >= this.cacheLength) {
                    logService.cam.error("GOPCache length exceeded (" + this.cacheLength + ")")
                }
                freePacket(pkt)
            }
        }
    }

    void writeKeyFrame(AVPacket pkt) {
        if (pkt == null || pkt.isNull()) return
        synchronized (cache) {
            for (int i = 0; i < packetIndex; ++i) {
                freePacket(cache[i])
                cache[i] = null
            }
            packetIndex = 0
            hasKeyFrame = true
            cache[packetIndex++] = pkt
        }
    }

    LinkedBlockingQueue<AVPacket> getQueue() {
        synchronized (cache) {
            LinkedBlockingQueue<AVPacket> q = new LinkedBlockingQueue<AVPacket>()
            for (int i = 0; i < packetIndex; ++i) {
                def pkt = cache[i]
                if (pkt != null && !pkt.isNull()) {
                    q.put(deepCopyPacket(pkt))
                    freePacket(pkt)
                }
                cache[i] = null
            }
            packetIndex = 0
            hasKeyFrame = false
            return q
        }
    }

    void cleanup() {
        synchronized (cache) {
            for (int i = 0; i < packetIndex; ++i) {
                if (cache[i] != null) {
                    freePacket(cache[i])
                    cache[i] = null
                }
            }
            packetIndex = 0
            hasKeyFrame = false
        }
    }
}
