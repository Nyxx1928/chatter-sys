package org.example.chat.listener;

import org.example.chat.repository.UserRepository;
import org.example.chat.service.UserPresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket event listener for connection lifecycle management.
 * 
 * This listener handles WebSocket connection and disconnection events to:
 * - Mark users as online when they connect
 * - Mark users as offline when they disconnect
 * - Publish presence updates to all chat rooms the user is a member of
 * 
 * Requirements: 2.2, 2.3, 7.1, 7.2, 7.3, 7.5
 */
@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final UserRepository userRepository;
    private final UserPresenceService userPresenceService;

    private final ConcurrentHashMap<String, AtomicInteger> connectionCounts = new ConcurrentHashMap<>();

    public WebSocketEventListener(
            UserRepository userRepository,
            UserPresenceService userPresenceService) {
        this.userRepository = userRepository;
        this.userPresenceService = userPresenceService;
    }

    /**
     * Handle WebSocket connection events.
     *
     * When a user establishes a WebSocket connection:
     * 1. Mark the user as online in the database
     * 2. Update the user's lastSeen timestamp
     * 3. Publish presence updates to all rooms the user is a member of
     *
     * @param event the session connect event
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();

        if (user != null) {
            String username = user.getName();
            int count = connectionCounts.computeIfAbsent(username, k -> new AtomicInteger(0)).incrementAndGet();
            logger.info("User connected: {} (active connections: {})", username, count);

            userRepository.findByUsername(username).ifPresent(userEntity -> {
                userPresenceService.markUserOnline(userEntity.getId());
            });
        }
    }

    /**
     * Handle WebSocket disconnection events.
     *
     * When a user disconnects from WebSocket:
     * 1. Mark the user as offline in the database ONLY if this was their last active connection
     * 2. Update the user's lastSeen timestamp
     * 3. Publish presence updates to all rooms the user is a member of
     *
     * @param event the session disconnect event
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();

        if (user != null) {
            String username = user.getName();
            AtomicInteger counter = connectionCounts.get(username);
            int remaining = (counter != null) ? counter.decrementAndGet() : 0;
            logger.info("User disconnected: {} (active connections: {})", username, remaining);

            if (remaining <= 0) {
                connectionCounts.remove(username);
                userRepository.findByUsername(username).ifPresent(userEntity -> {
                    userPresenceService.markUserOffline(userEntity.getId());
                });
            }
        }
    }
}
