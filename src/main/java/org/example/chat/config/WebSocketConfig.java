package org.example.chat.config;

import org.example.chat.security.WebSocketAuthenticationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for STOMP messaging.
 * 
 * This configuration enables WebSocket communication using STOMP protocol
 * for real-time messaging in the chat system.
 * 
 * Requirements: 2.1, 12.2
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthenticationInterceptor authenticationInterceptor;

    public WebSocketConfig(WebSocketAuthenticationInterceptor authenticationInterceptor) {
        this.authenticationInterceptor = authenticationInterceptor;
    }

    /**
     * Register STOMP endpoints that clients will use to connect to the WebSocket server.
     * 
     * Configures:
     * - Endpoint at /ws for WebSocket connections
     * - SockJS fallback for browsers that don't support WebSocket
     * - CORS allowed origins for frontend application
     * 
     * @param registry the STOMP endpoint registry
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:3000")
                .withSockJS();
    }

    /**
     * Configure the message broker for routing messages.
     * 
     * Configures:
     * - Simple in-memory broker with /topic prefix for pub/sub messaging
     * - Simple in-memory broker with /queue prefix for point-to-point messaging
     * - Application destination prefix /app for messages bound for @MessageMapping methods
     * - User destination prefix /user for user-specific messages
     * 
     * @param registry the message broker registry
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable a simple in-memory message broker with /topic and /queue prefixes
        registry.enableSimpleBroker("/topic", "/queue");
        
        // Set prefix for messages bound for @MessageMapping-annotated methods
        registry.setApplicationDestinationPrefixes("/app");
        
        // Set prefix for user-specific destinations
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Configure client inbound channel to add authentication interceptor.
     * The interceptor validates JWT tokens from STOMP CONNECT frames.
     * 
     * @param registration the channel registration
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authenticationInterceptor);
    }
}
