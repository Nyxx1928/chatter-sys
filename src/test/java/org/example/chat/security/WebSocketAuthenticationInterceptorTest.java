package org.example.chat.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebSocketAuthenticationInterceptor.
 * 
 * Verifies that JWT tokens are properly validated during STOMP CONNECT frames
 * and that invalid or missing tokens result in connection rejection.
 */
@ExtendWith(MockitoExtension.class)
class WebSocketAuthenticationInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private MessageChannel messageChannel;

    private WebSocketAuthenticationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthenticationInterceptor(jwtUtil, userDetailsService);
    }

    @Test
    void preSend_ValidToken_SetsAuthenticatedUser() {
        // Arrange
        String validToken = "valid.jwt.token";
        String username = "testuser";
        
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + validToken);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        UserDetails userDetails = new User(username, "password", 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.getUsernameFromToken(validToken)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

        // Act
        Message<?> result = interceptor.preSend(message, messageChannel);

        // Assert
        assertNotNull(result);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNotNull(resultAccessor.getUser());
        assertTrue(resultAccessor.getUser() instanceof UsernamePasswordAuthenticationToken);
        
        UsernamePasswordAuthenticationToken auth = 
            (UsernamePasswordAuthenticationToken) resultAccessor.getUser();
        assertEquals(username, ((UserDetails) auth.getPrincipal()).getUsername());

        verify(jwtUtil).validateToken(validToken);
        verify(jwtUtil).getUsernameFromToken(validToken);
        verify(userDetailsService).loadUserByUsername(username);
    }

    @Test
    void preSend_MissingToken_RejectsConnection() {
        // Arrange
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        // No Authorization header
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // Act
        Message<?> result = interceptor.preSend(message, messageChannel);

        // Assert
        assertNull(result, "Connection should be rejected when token is missing");
        verify(jwtUtil, never()).validateToken(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void preSend_InvalidToken_RejectsConnection() {
        // Arrange
        String invalidToken = "invalid.jwt.token";
        
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + invalidToken);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtUtil.validateToken(invalidToken)).thenReturn(false);

        // Act
        Message<?> result = interceptor.preSend(message, messageChannel);

        // Assert
        assertNull(result, "Connection should be rejected when token is invalid");
        verify(jwtUtil).validateToken(invalidToken);
        verify(jwtUtil, never()).getUsernameFromToken(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void preSend_TokenWithoutBearerPrefix_RejectsConnection() {
        // Arrange
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "invalid.jwt.token");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // Act
        Message<?> result = interceptor.preSend(message, messageChannel);

        // Assert
        assertNull(result, "Connection should be rejected when token doesn't have Bearer prefix");
        verify(jwtUtil, never()).validateToken(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void preSend_UserDetailsServiceThrowsException_RejectsConnection() {
        // Arrange
        String validToken = "valid.jwt.token";
        String username = "testuser";
        
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + validToken);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.getUsernameFromToken(validToken)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username))
            .thenThrow(new RuntimeException("User not found"));

        // Act
        Message<?> result = interceptor.preSend(message, messageChannel);

        // Assert
        assertNull(result, "Connection should be rejected when user details cannot be loaded");
        verify(jwtUtil).validateToken(validToken);
        verify(jwtUtil).getUsernameFromToken(validToken);
        verify(userDetailsService).loadUserByUsername(username);
    }

    @Test
    void preSend_NonConnectCommand_PassesThrough() {
        // Arrange
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // Act
        Message<?> result = interceptor.preSend(message, messageChannel);

        // Assert
        assertNotNull(result);
        assertEquals(message, result);
        verify(jwtUtil, never()).validateToken(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void preSend_NullAccessor_PassesThrough() {
        // Arrange
        Message<?> message = MessageBuilder.withPayload(new byte[0]).build();

        // Act
        Message<?> result = interceptor.preSend(message, messageChannel);

        // Assert
        assertNotNull(result);
        assertEquals(message, result);
        verify(jwtUtil, never()).validateToken(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }
}
