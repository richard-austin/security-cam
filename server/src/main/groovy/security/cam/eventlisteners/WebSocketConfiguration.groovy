package security.cam.eventlisteners

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer{
    @Override
    void configureMessageBroker(MessageBrokerRegistry registry) {

        ThreadPoolTaskScheduler te = new ThreadPoolTaskScheduler()
        te.setPoolSize(1)
        te.setThreadNamePrefix("wss-heartbeat-thread-");
        te.initialize()

        registry.enableSimpleBroker("/topic")
        .setTaskScheduler(te)
        .heartbeatValue = [120000, 120000]
        registry.setApplicationDestinationPrefixes("/app")
    }

    @Override
    void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/stomp").setAllowedOriginPatterns("*")
        registry.addEndpoint("/audio").setAllowedOriginPatterns("*")
    }
}
