package com.securitycam.configuration

import com.securitycam.beans.MediaStreamHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket // Enables native, low-level web socket configuration
class RawWebSocketConfig implements WebSocketConfigurer {

    private final MediaStreamHandler mediaStreamHandler

    RawWebSocketConfig(MediaStreamHandler mediaStreamHandler) {
        this.mediaStreamHandler = mediaStreamHandler
    }

    @Override
    void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Match the static base path before the '?' query character
        registry.addHandler(mediaStreamHandler, "/ws/stream")
                .setAllowedOrigins("*")
    }
}
