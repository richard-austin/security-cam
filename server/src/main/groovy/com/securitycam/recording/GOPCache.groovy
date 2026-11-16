package com.securitycam.recording

import com.securitycam.services.LogService
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avformat.AVFormatContext
import org.bytedeco.ffmpeg.avformat.AVStream

import java.util.concurrent.LinkedBlockingQueue

import static com.securitycam.recording.deepCopy.deepCopyPacket
import static com.securitycam.recording.deepCopy.freePacket
import static org.bytedeco.ffmpeg.global.avcodec.AV_PKT_FLAG_KEY
import org.bytedeco.ffmpeg.global.avutil

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free

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
        synchronized (cache) {
            if (packetIndex < this.cacheLength) {
                if (hasKeyFrame)
                    cache[packetIndex++] = pkt     // Only keyFrames at the start
            } else
                logService.cam.error("GOPCache length exceeded (" + this.cacheLength + ")")
        }
    }

    void writeKeyFrame(AVPacket pkt) {
        hasKeyFrame = true
        synchronized (cache) {
            for (int i = 0; i < packetIndex; ++i) {
                def cachedPkt = cache[i]
                freePacket(cachedPkt)
            }
            packetIndex = 0
        }
        write(pkt)
    }

    LinkedBlockingQueue getQueue() {
        synchronized (cache) {
            LinkedBlockingQueue<AVPacket> q = new LinkedBlockingQueue<AVPacket>()

            // Put the current gop cache into the queue
            for (int i = 0; i < packetIndex; ++i) {
                def pkt = cache[i]
                q.put(deepCopyPacket(pkt))
                freePacket(pkt)
            }

            packetIndex = 0
            hasKeyFrame = false
            return q
        }
    }

}
