package com.securitycam.mediaserverutils

import org.springframework.web.socket.BinaryMessage
import org.springframework.web.socket.WebSocketSession

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ClientStream {
    WebSocketSession session
    PacketQueue queue

    ExecutorService outputThread
    Queue audioDetails

    ClientStream(WebSocketSession session, GOPCache cache, String codecString) {
        audioDetails = new ConcurrentLinkedQueue()
        audioDetails.add(codecString)
        this.session = session
        this.queue = new PacketQueue(cache, codecString)
        outputThread = Executors.newSingleThreadExecutor()
         outputThread.submit(() -> {
            for(;;) {
                if(!session.isOpen())
                    break
                session.sendMessage((queue.getNextPacket()))
            }
        })
    }

    void put(byte[] pkt) {
        queue.putPacket(pkt)
    }

    void close() {
        outputThread.shutdown()
    }

    void putAudio(byte[] pkt) {
        if(session.isOpen()) {
            if (!audioDetails.isEmpty()) {
                session.sendMessage(new BinaryMessage(audioDetails.remove() as byte[]))
            } else
                session.sendMessage(new BinaryMessage(pkt))
        }
    }
}
