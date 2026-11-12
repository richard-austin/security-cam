package com.securitycam.beans

import com.securitycam.mediaserverutils.Stream
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.BinaryWebSocketHandler
import java.util.concurrent.ConcurrentHashMap

@Component
class MediaStreamHandler extends BinaryWebSocketHandler {

    // Global registry mapping suuid -> Set of listening browser sessions
    Map<String, Stream> streams

    MediaStreamHandler() {
        streams = new ConcurrentHashMap<>()
    }

    @Override
    void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String suuid = getSuuid(session)
        Stream stream = streams.get(suuid)
        if(stream != null)
            stream.addClient(session)
    }

    void addStream(String suuid, String codecString) {
        if(!streams.containsKey(suuid))
            streams.put(suuid, new Stream(suuid, codecString, false))
    }

    void addAudioStream(String suuid, String json) {
        if(!streams.containsKey(suuid))
            streams.put(suuid, new Stream(suuid, json, true))
    }

    /**
     * sendPacket for video
     */
    void sendPacket(String suuid, byte[] packetData, boolean isKeyFrame) {
        streams[suuid].put(packetData, isKeyFrame)
    }

    /**
     *   sendPacket for audio
     */
    void sendPacket(String suuid, byte[] packetData) {
        streams[suuid].put(packetData)
    }

    @Override
    void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String suuid = getSuuid(session)
        Stream stream = streams.get(suuid)
        if(stream != null) {
            stream.removeClient(session)
        }
    }

    private static String getSuuid(WebSocketSession session) {
        URI uri = session.getUri()
        if (uri == null || uri.getQuery() == null) return null
        return Arrays.stream(uri.getQuery().split("&"))
                .map(param -> param.split("="))
                .filter(pair -> pair.length > 1 && "suuid".equals(pair[0]))
                .map(pair -> pair[1])
                .findFirst()
                .orElse(null)
    }
}
