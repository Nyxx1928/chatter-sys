package org.example.chat.config;

import org.example.chat.security.WebSocketAuthenticationInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * WebSocket configuration for STOMP messaging.
 *
 * Requirements: 2.1, 12.2
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthenticationInterceptor authenticationInterceptor;

    /**
     * Extra origins injected from the CORS_ALLOWED_ORIGINS environment variable.
     * Same variable used by SecurityConfig so you only need to set it once.
     */
    @Value("${cors.allowed-origins:}")
    private String corsAllowedOriginsEnv;

    public WebSocketConfig(WebSocketAuthenticationInterceptor authenticationInterceptor) {
        this.authenticationInterceptor = authenticationInterceptor;
    }

    /**
     * Builds the list of allowed WebSocket origins.
     * Always includes localhost:3000; additional origins come from the env var.
     */
    private String[] buildAllowedOrigins() {
        List<String> origins = new ArrayList<>(List.of("http://localhost:3000"));
        if (corsAllowedOriginsEnv != null && !corsAllowedOriginsEnv.isBlank()) {
            Arrays.stream(corsAllowedOriginsEnv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(origins::add);
        }
        return origins.toArray(new String[0]);
    }

    /**
     * Register STOMP endpoints that clients will use to connect to the WebSocket
     * server.
     *
     * @param registry the STOMP endpoint registry
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(buildAllowedOrigins())
                .withSockJS();
    }

    /**
     * Configure the message broker for routing messages.
     *
     * @param registry the message broker registry
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Configure client inbound channel to add authentication interceptor.
     *
     * @param registration the channel registration
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authenticationInterceptor);
    }
}
