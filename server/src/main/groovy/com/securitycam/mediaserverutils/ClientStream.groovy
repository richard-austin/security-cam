package com.securitycam.mediaserverutils

import org.springframework.web.socket.WebSocketSession

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ClientStream {
    WebSocketSession session
    PacketQueue queue

    ExecutorService outputThread

    ClientStream(WebSocketSession session, GOPCache cache, String codecString) {
        this.session = session
        this.queue = new PacketQueue(cache, codecString)
        outputThread = Executors.newSingleThreadExecutor()
         outputThread.submit(() -> {
            for(;;)
                session.sendMessage((queue.getNextPacket()))
        })
    }

    void put(byte[] pkt) {
        queue.putPacket(pkt)
    }

    void close() {
        outputThread.shutdown()
        outputThread.close()
    }
}
