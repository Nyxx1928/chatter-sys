package org.example.chat.listener;

import org.example.chat.entity.RoomMembership;
import org.example.chat.entity.User;
import org.example.chat.repository.RoomMembershipRepository;
import org.example.chat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final RoomMembershipRepository roomMembershipRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventListener(
            UserRepository userRepository,
            RoomMembershipRepository roomMembershipRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.userRepository = userRepository;
        this.roomMembershipRepository = roomMembershipRepository;
        this.messagingTemplate = messagingTemplate;
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
            logger.info("User connected: {}", username);

            // Mark user as online
            userRepository.findByUsername(username).ifPresent(userEntity -> {
                userEntity.setOnline(true);
                userEntity.setLastSeen(LocalDateTime.now());
                userRepository.save(userEntity);

                // Publish presence updates to all rooms the user is a member of
                publishPresenceUpdate(userEntity, true);
            });
        }
    }

    /**
     * Handle WebSocket disconnection events.
     * 
     * When a user disconnects from WebSocket:
     * 1. Mark the user as offline in the database
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
            logger.info("User disconnected: {}", username);

            // Mark user as offline
            userRepository.findByUsername(username).ifPresent(userEntity -> {
                userEntity.setOnline(false);
                userEntity.setLastSeen(LocalDateTime.now());
                userRepository.save(userEntity);

                // Publish presence updates to all rooms the user is a member of
                publishPresenceUpdate(userEntity, false);
            });
        }
    }

    /**
     * Publish presence update to all chat rooms the user is a member of.
     * 
     * Sends a presence update message to /topic/presence/{roomId} for each room
     * the user belongs to, notifying other members of the user's online/offline status.
     * 
     * @param user the user whose presence changed
     * @param online true if user is now online, false if offline
     */
    private void publishPresenceUpdate(User user, boolean online) {
        // Find all rooms the user is a member of
        List<RoomMembership> memberships = roomMembershipRepository.findByUser(user);

        // Publish presence update to each room
        for (RoomMembership membership : memberships) {
            Long roomId = membership.getChatRoom().getId();
            
            Map<String, Object> presenceUpdate = new HashMap<>();
            presenceUpdate.put("userId", user.getId());
            presenceUpdate.put("username", user.getUsername());
            presenceUpdate.put("displayName", user.getDisplayName());
            presenceUpdate.put("online", online);
            presenceUpdate.put("lastSeen", user.getLastSeen().toString());
            presenceUpdate.put("roomId", roomId);

            String destination = "/topic/presence/" + roomId;
            messagingTemplate.convertAndSend(destination, presenceUpdate);
            
            logger.debug("Published presence update for user {} to room {}: online={}", 
                    user.getUsername(), roomId, online);
        }
    }
}
