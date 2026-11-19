package com.securitycam.mediaserverutils

import com.securitycam.services.LogService
import org.springframework.web.socket.WebSocketSession

class Stream {
    String suuid
    GOPCache cache
    private final gopCacheSize = 200
    private final String codecString
    private isAudio
    private LogService logService

    Map<WebSocketSession, ClientStream> clients
    Stream(String suuid, String codecString, boolean isAudio, LogService logService) {
        this.logService = logService
        this.isAudio = isAudio
        this.suuid = suuid
        cache = !isAudio ? new GOPCache(gopCacheSize) : null
        clients = new HashMap<>()
        this.codecString = codecString
    }

    void addClient(WebSocketSession session) {
        if (!clients.containsKey(session)) {
            clients.put(session, new ClientStream(session, cache, codecString, logService))
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
