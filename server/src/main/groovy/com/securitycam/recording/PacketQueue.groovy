package com.securitycam.recording

import org.bytedeco.ffmpeg.avcodec.AVPacket

import java.util.concurrent.BlockingQueue

import static com.securitycam.recording.deepCopy.freePacket

class PacketQueue {
    private static final AVPacket POISON_PILL = new AVPacket()

    final int cacheLength
    private volatile boolean stopped
    private final BlockingQueue<AVPacket> q
    private final Object lock = new Object()

    PacketQueue(GOPCache src) {
        this.stopped = false
        this.cacheLength = src.cacheLength
        this.q = src.getQueue()
    }

    AVPacket getNextPacket() throws InterruptedException {
        AVPacket pkt = q.take()
        if (pkt === POISON_PILL) {
            return null
        }
        return pkt
    }

    void putPacket(AVPacket pkt) {
        if (pkt == null || pkt.isNull()) return
        synchronized (lock) {
            if (!stopped) {
                q.put(pkt)
            } else {
                freePacket(pkt)
            }
        }
    }

    void stop() {
        synchronized (lock) {
            if (stopped) return
            stopped = true
            AVPacket pkt
            while ((pkt = q.poll()) != null) {
                if (pkt !== POISON_PILL) {
                    freePacket(pkt)
                }
            }
            q.offer(POISON_PILL)
        }
    }
}
