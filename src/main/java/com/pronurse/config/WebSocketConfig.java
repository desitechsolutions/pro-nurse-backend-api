package com.pronurse.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Mobile clients will subscribe to paths starting with /queue (private) or /topic (public)
        config.enableSimpleBroker("/queue", "/topic");

        // Prefix for messages sent FROM the mobile client TO the server (if needed)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // The raw WebSocket connection URL for Flutter: ws://your-server/ws-alerts
        registry.addEndpoint("/ws-alerts")
                .setAllowedOriginPatterns("*");
        // Note: .withSockJS() is omitted because Flutter typically uses raw STOMP over WebSockets
    }
}