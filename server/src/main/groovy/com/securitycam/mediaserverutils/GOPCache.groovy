package com.securitycam.mediaserverutils
import java.util.concurrent.LinkedBlockingQueue

class GOPCache {
    private int index
    private ArrayList<byte[]> cache
    int cacheLength

    GOPCache(int cacheLength) {
        this.cacheLength = cacheLength
        index = 0
        cache = new ArrayList<byte[]>(cacheLength)
    }

    void put(byte[] packet, boolean isKeyFrame) {
        if (index > 0 || isKeyFrame) {
            if (isKeyFrame) {
                index = 0
                cache.clear()
            }
            cache[index++] = packet
        }
    }

    LinkedBlockingQueue getQueue(String codecString) {
        LinkedBlockingQueue<byte[]> q = new LinkedBlockingQueue<byte[]>()

        // Send codec string first with 9 prepended
        byte[] nine = [9]
        byte[] codecBytes = codecString.getBytes()
        byte[] firstPacket = new byte[nine.length + codecBytes.length]
        firstPacket[0] = 9

        for(int i=nine.length;i<codecBytes.length+nine.length;i++){
            firstPacket[i] = codecBytes[i-nine.length]
        }
        q.put(firstPacket)

        // Put the current gop cache into the queue
        for(int i = 0; i < index; ++i)
            q.put(cache[i])

        return q
    }
}

