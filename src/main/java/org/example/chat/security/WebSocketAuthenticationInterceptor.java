package org.example.chat.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * WebSocket authentication interceptor that validates JWT tokens from STOMP CONNECT frames.
 * 
 * This interceptor extracts the JWT token from the Authorization header in STOMP CONNECT frames,
 * validates the token, and sets the authenticated user principal in the STOMP session.
 * Connections with invalid or missing tokens are rejected.
 * 
 * Requirements: 1.5, 2.1
 */
@Component
public class WebSocketAuthenticationInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthenticationInterceptor.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public WebSocketAuthenticationInterceptor(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Intercepts messages before they are sent to the message channel.
     * For STOMP CONNECT frames, extracts and validates the JWT token from the Authorization header.
     * Sets the authenticated user principal in the STOMP session if the token is valid.
     * Rejects connections with invalid or missing tokens.
     *
     * @param message the message being sent
     * @param channel the message channel
     * @return the message to continue processing, or null to reject the message
     */
    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            logger.debug("Processing STOMP CONNECT frame");

            try {
                // Extract JWT token from Authorization header
                String jwt = extractJwtFromHeaders(accessor);

                if (jwt == null) {
                    logger.error("Missing Authorization header in STOMP CONNECT frame");
                    throw new IllegalArgumentException("Missing authentication token");
                }

                // Validate the JWT token
                if (!jwtUtil.validateToken(jwt)) {
                    logger.error("Invalid JWT token in STOMP CONNECT frame");
                    throw new IllegalArgumentException("Invalid authentication token");
                }

                // Extract username from token
                String username = jwtUtil.getUsernameFromToken(jwt);
                logger.debug("Extracted username from JWT: {}", username);

                // Load user details
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Create authentication token
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );

                // Set the authenticated user principal in the STOMP session
                accessor.setUser(authentication);
                
                logger.info("WebSocket connection authenticated for user: {}", username);

            } catch (Exception ex) {
                logger.error("WebSocket authentication failed: {}", ex.getMessage());
                // Return null to reject the connection
                return null;
            }
        }

        return message;
    }

    /**
     * Extracts JWT token from the Authorization header in STOMP headers.
     *
     * @param accessor the STOMP header accessor
     * @return the JWT token, or null if not present or invalid format
     */
    private String extractJwtFromHeaders(StompHeaderAccessor accessor) {
        String bearerToken = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
        
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }
}
