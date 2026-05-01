package org.example.chat.service;

import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.RoomMembership;
import org.example.chat.entity.User;
import org.example.chat.repository.RoomMembershipRepository;
import org.example.chat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for tracking and managing user presence (online/offline status).
 * Handles presence updates when users connect/disconnect and broadcasts status changes.
 */
@Service
public class UserPresenceService {

    private static final Logger logger = LoggerFactory.getLogger(UserPresenceService.class);

    private final UserRepository userRepository;
    private final RoomMembershipRepository roomMembershipRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public UserPresenceService(UserRepository userRepository,
                              RoomMembershipRepository roomMembershipRepository,
                              SimpMessagingTemplate messagingTemplate) {
        this.userRepository = userRepository;
        this.roomMembershipRepository = roomMembershipRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Marks a user as online and updates their last seen timestamp.
     * Publishes presence updates to all rooms where the user is a member.
     *
     * @param userId the ID of the user to mark as online
     */
    @Transactional
    public void markUserOnline(Long userId) {
        logger.info("Marking user ID: {} as online", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> {
                logger.warn("Mark user online failed: user not found: {}", userId);
                return new IllegalArgumentException("User not found");
            });

        user.setOnline(true);
        user.setLastSeen(LocalDateTime.now());
        userRepository.save(user);

        logger.info("User ID: {} marked as online at {}", userId, user.getLastSeen());

        // Publish presence updates to all rooms where user is a member
        publishPresenceUpdates(user);
    }

    /**
     * Marks a user as offline and updates their last seen timestamp.
     * Publishes presence updates to all rooms where the user is a member.
     *
     * @param userId the ID of the user to mark as offline
     */
    @Transactional
    public void markUserOffline(Long userId) {
        logger.info("Marking user ID: {} as offline", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> {
                logger.warn("Mark user offline failed: user not found: {}", userId);
                return new IllegalArgumentException("User not found");
            });

        user.setOnline(false);
        user.setLastSeen(LocalDateTime.now());
        userRepository.save(user);

        logger.info("User ID: {} marked as offline at {}", userId, user.getLastSeen());

        // Publish presence updates to all rooms where user is a member
        publishPresenceUpdates(user);
    }

    /**
     * Publishes presence updates to all rooms where the user is a member.
     * Sends a presence payload to the /topic/presence/{roomId} destination for each room.
     *
     * @param user the user whose presence status changed
     */
    public void publishPresenceUpdate(User user) {
        logger.debug("Publishing presence update for user ID: {}", user.getId());

        List<RoomMembership> memberships = roomMembershipRepository.findByUser(user);

        for (RoomMembership membership : memberships) {
            ChatRoom room = membership.getChatRoom();
            String destination = "/topic/presence/" + room.getId();

            Map<String, Object> presencePayload = createPresencePayload(user, room.getId());

            logger.debug("Sending presence update to topic: {} for user: {}", destination, user.getUsername());
            messagingTemplate.convertAndSend(destination, presencePayload);
        }

        logger.info("Presence updates published for user ID: {} to {} rooms", user.getId(), memberships.size());
    }

    /**
     * Retrieves all online users in a specific chat room.
     *
     * @param roomId the ID of the chat room
     * @return list of online users in the room
     */
    public List<User> getOnlineUsers(Long roomId) {
        logger.debug("Retrieving online users for room ID: {}", roomId);

        List<RoomMembership> memberships = roomMembershipRepository.findByChatRoom(
            new ChatRoom() {{ setId(roomId); }}
        );

        List<User> onlineUsers = memberships.stream()
            .map(RoomMembership::getUser)
            .filter(User::getOnline)
            .collect(Collectors.toList());

        logger.debug("Found {} online users in room ID: {}", onlineUsers.size(), roomId);

        return onlineUsers;
    }

    /**
     * Private helper method to publish presence updates to all rooms.
     * This is called by markUserOnline and markUserOffline.
     *
     * @param user the user whose presence status changed
     */
    private void publishPresenceUpdates(User user) {
        publishPresenceUpdate(user);
    }

    /**
     * Creates a presence payload for broadcasting.
     *
     * @param user the user whose presence changed
     * @param roomId the room ID where the presence update is being sent
     * @return map containing presence information
     */
    private Map<String, Object> createPresencePayload(User user, Long roomId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", user.getId());
        payload.put("username", user.getUsername());
        payload.put("displayName", user.getDisplayName());
        payload.put("online", user.getOnline());
        payload.put("lastSeen", user.getLastSeen());
        payload.put("roomId", roomId);
        return payload;
    }
}
