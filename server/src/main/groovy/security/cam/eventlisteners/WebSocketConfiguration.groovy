package security.cam.eventlisteners

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer{
    @Override
    void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic")
        registry.setApplicationDestinationPrefixes("/app")
    }

    @Override
    void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/stomp").setAllowedOriginPatterns("*").withSockJS()
        registry.addEndpoint("/stomp").setAllowedOriginPatterns("*")
        registry.addEndpoint("/audio").setAllowedOriginPatterns("*").withSockJS()
        registry.addEndpoint("/audio").setAllowedOriginPatterns("*")
    }
}
