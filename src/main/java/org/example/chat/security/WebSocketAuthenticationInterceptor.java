package org.example.chat.security;

import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.User;
import org.example.chat.repository.ChatRoomRepository;
import org.example.chat.repository.RoomMembershipRepository;
import org.example.chat.repository.UserRepository;
import org.example.chat.util.SecurityAuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern ROOM_ID_DESTINATION = Pattern.compile(
            "^/(topic/(room|presence)|app/(chat\\.send|room\\.(join|leave)))/(?<roomId>\\d+)(/.*)?$");

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final RoomMembershipRepository roomMembershipRepository;
    private final SecurityAuditLogger securityAuditLogger;

    public WebSocketAuthenticationInterceptor(
            JwtUtil jwtUtil,
            UserDetailsService userDetailsService,
            UserRepository userRepository,
            ChatRoomRepository chatRoomRepository,
            RoomMembershipRepository roomMembershipRepository,
            SecurityAuditLogger securityAuditLogger) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.roomMembershipRepository = roomMembershipRepository;
        this.securityAuditLogger = securityAuditLogger;
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

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
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

        // Authorization checks for room-scoped destinations to prevent subscription leaks
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) || StompCommand.SEND.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination == null || destination.isBlank()) {
                return message;
            }

            Optional<Long> roomId = extractRoomId(destination);
            if (roomId.isEmpty()) {
                return message;
            }

            Principal principal = accessor.getUser();
            if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
                logger.warn("Rejecting {} to {}: missing authenticated principal",
                        accessor.getCommand(), destination);
                return null;
            }

            try {
                enforceRoomMembership(principal.getName(), roomId.get(), destination);
            } catch (MessagingException ex) {
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

    private Optional<Long> extractRoomId(String destination) {
        Matcher matcher = ROOM_ID_DESTINATION.matcher(destination);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(matcher.group("roomId")));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private void enforceRoomMembership(String username, Long roomId, String destination) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new MessagingException("Unknown user for WebSocket principal"));

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new MessagingException("Unknown room destination"));

        if (roomMembershipRepository.findByUserAndChatRoom(user, room).isEmpty()) {
            logger.warn("Rejecting unauthorized WebSocket access: user {} to destination {}", user.getId(), destination);
            securityAuditLogger.logAuthorizationFailure(user.getId(), roomId,
                    "Unauthorized WebSocket access to destination: " + destination);
            throw new MessagingException("User is not a member of this room");
        }
    }
}
