package com.securitycam.mediaserverutils

import org.springframework.web.socket.WebSocketSession

import java.util.concurrent.ConcurrentLinkedQueue

class Stream {
    String suuid
    GOPCache cache
    private final gopCacheSize = 200
    private final String codecString

    Map<WebSocketSession, ClientStream> clients
    Stream(String suuid, String codecString) {
        this.suuid = suuid
        cache = new GOPCache(gopCacheSize)
        clients = new HashMap<>()
        this.codecString = codecString
    }

    void addClient(WebSocketSession session) {
        if (!clients.containsKey(session)) {
            clients.put(session, new ClientStream(session, cache, codecString))
        }
    }

    void removeClient(WebSocketSession session) {
        ClientStream stream = clients.get(session)

        stream.close()
        clients.remove(session)
    }

    /**
     * put for video
     * @param pkt
     * @param isKeyFrame
     */
    void put(byte[] pkt, boolean isKeyFrame) {
        cache.put(pkt, isKeyFrame)
        clients.forEach { key, client -> {
                client.put(pkt)
            }
        }
    }

    /**
     * put for audio
     * @param pkt
     */
    void put(byte[] pkt) {
        clients.forEach { key, client -> {
                client.putAudio(pkt)
            }
        }
    }
}
