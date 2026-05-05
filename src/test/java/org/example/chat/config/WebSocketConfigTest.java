package org.example.chat.config;

import org.example.chat.security.WebSocketAuthenticationInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.mockito.Mockito.*;

/**
 * Unit tests for WebSocketConfig.
 * 
 * Verifies that the WebSocket configuration is set up correctly
 * with proper STOMP endpoints and message broker settings.
 */
class WebSocketConfigTest {

    @Test
    void registerStompEndpoints_ConfiguresWebSocketEndpoint() {
        // Arrange
        WebSocketAuthenticationInterceptor authInterceptor = mock(WebSocketAuthenticationInterceptor.class);
        WebSocketConfig config = new WebSocketConfig(authInterceptor);
        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration registration = mock(StompWebSocketEndpointRegistration.class, RETURNS_SELF);
        
        when(registry.addEndpoint("/ws")).thenReturn(registration);
        
        // Act
        config.registerStompEndpoints(registry);
        
        // Assert
        verify(registry).addEndpoint("/ws");
        verify(registration).setAllowedOrigins("http://localhost:3000", "https://chatter-sys.vercel.app");
        verify(registration).withSockJS();
    }

    @Test
    void configureMessageBroker_ConfiguresBrokerPrefixes() {
        // Arrange
        WebSocketAuthenticationInterceptor authInterceptor = mock(WebSocketAuthenticationInterceptor.class);
        WebSocketConfig config = new WebSocketConfig(authInterceptor);
        MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);
        
        // Act
        config.configureMessageBroker(registry);
        
        // Assert
        verify(registry).enableSimpleBroker("/topic", "/queue");
        verify(registry).setApplicationDestinationPrefixes("/app");
        verify(registry).setUserDestinationPrefix("/user");
    }

    @Test
    void configureClientInboundChannel_RegistersAuthenticationInterceptor() {
        // Arrange
        WebSocketAuthenticationInterceptor authInterceptor = mock(WebSocketAuthenticationInterceptor.class);
        WebSocketConfig config = new WebSocketConfig(authInterceptor);
        ChannelRegistration registration = mock(ChannelRegistration.class);
        
        // Act
        config.configureClientInboundChannel(registration);
        
        // Assert
        verify(registration).interceptors(authInterceptor);
    }
}
