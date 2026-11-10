package com.securitycam.mediaserverutils

import org.springframework.web.socket.BinaryMessage

import java.util.concurrent.BlockingQueue

class PacketQueue {
    private int index
    private ArrayList<byte[]> cacheSnapshot
    int cacheLength

    BlockingQueue q

    PacketQueue(GOPCache src, String codecString) {
        this.cacheLength = src.cacheLength
        this.cacheSnapshot = new ArrayList<byte[]>(src.cacheLength)
        this.index = 0
        this.q = src.getQueue(codecString)
    }

    BinaryMessage getNextPacket() {
        return new BinaryMessage(q.take() as byte[])
    }

    void putPacket(byte[] pkt) {
        q.put(pkt)
    }
}
