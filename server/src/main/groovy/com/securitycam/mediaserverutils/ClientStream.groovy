package com.securitycam.mediaserverutils

import org.springframework.web.socket.BinaryMessage
import org.springframework.web.socket.WebSocketSession

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ClientStream {
    WebSocketSession session
    PacketQueue queue

    ExecutorService outputThread = null
    byte[] firstPacket
    boolean firstPacketSent = false

    ClientStream(WebSocketSession session, GOPCache cache, String codecString) {
        byte[] codecBytes = codecString.getBytes()
        firstPacket = new byte[1 + codecBytes.length]
        firstPacket[0] = 9

        for(int i=1;i<codecBytes.length+1;i++){
            firstPacket[i] = codecBytes[i-1]
        }
        this.session = session
        if(cache != null) {
            this.queue = new PacketQueue(cache, codecString)
            outputThread = Executors.newSingleThreadExecutor()
            outputThread.submit(() -> {
                for (; ;) {
                    if (!session.isOpen())
                        break
                    session.sendMessage((queue.getNextPacket()))
                }
            })
        }
    }

    void put(byte[] pkt) {
        queue.putPacket(pkt)
    }

    void close() {
        if(outputThread != null)
            outputThread.shutdown()
    }

    void putAudio(byte[] pkt) {
        if(session.isOpen()) {
            if (!firstPacketSent) {
                session.sendMessage(new BinaryMessage(firstPacket))
                firstPacketSent = true
            } else
                session.sendMessage(new BinaryMessage(pkt))
        }
    }
}
