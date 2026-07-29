package com.featureforge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP-over-WebSocket for pushing flag changes to connected dashboard
 * clients in real time, instead of the Angular app polling on an interval.
 *
 * Topic shape: /topic/projects/{projectId}/flags — a client subscribes once
 * per project it's viewing and gets every create/update/delete for flags in
 * that project as they happen (see FlagBroadcastService).
 *
 * SockJS fallback is included so it degrades gracefully behind proxies/older
 * browsers that don't support raw WebSocket upgrade.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
