package org.example.chat.service;

import org.example.chat.dto.MessageResponse;
import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.Message;
import org.example.chat.entity.RoomMembership;
import org.example.chat.entity.User;
import org.example.chat.repository.ChatRoomRepository;
import org.example.chat.repository.MessageRepository;
import org.example.chat.repository.RoomMembershipRepository;
import org.example.chat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for managing chat messages.
 * Handles message sending, validation, persistence, broadcasting, and history
 * retrieval.
 */
@Service
public class ChatMessageService {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageService.class);
    private static final int MAX_MESSAGE_LENGTH = 5000;

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final RoomMembershipRepository roomMembershipRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatMessageService(MessageRepository messageRepository,
            ChatRoomRepository chatRoomRepository,
            UserRepository userRepository,
            RoomMembershipRepository roomMembershipRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.userRepository = userRepository;
        this.roomMembershipRepository = roomMembershipRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Sends a message to a chat room.
     * Validates the message, checks membership, persists to database, and
     * broadcasts to STOMP topic.
     *
     * @param senderId the ID of the user sending the message
     * @param roomId   the ID of the chat room
     * @param content  the message content
     * @return the persisted Message entity
     * @throws IllegalArgumentException if validation fails or sender is not a
     *                                  member
     */
    @Transactional
    public Message sendMessage(Long senderId, Long roomId, String content) {
        logger.info("User ID: {} attempting to send message to room ID: {}", senderId, roomId);

        // Validate message content
        validateMessageContent(content);

        // Find sender
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> {
                    logger.warn("Send message failed: sender not found: {}", senderId);
                    return new IllegalArgumentException("Sender not found");
                });

        // Find chat room
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> {
                    logger.warn("Send message failed: chat room not found: {}", roomId);
                    return new IllegalArgumentException("Chat room not found");
                });

        // Validate sender is a member of the chat room
        validateMembership(sender, chatRoom);

        // Create and persist message
        Message message = new Message();
        message.setSender(sender);
        message.setChatRoom(chatRoom);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);
        logger.info("Message persisted with ID: {} in room ID: {}", savedMessage.getId(), roomId);

        // Broadcast message to STOMP topic
        broadcastMessage(savedMessage);

        return savedMessage;
    }

    /**
     * Retrieves paginated message history for a chat room.
     *
     * @param roomId   the ID of the chat room
     * @param pageable pagination parameters
     * @return page of messages ordered by timestamp descending
     * @throws IllegalArgumentException if room is not found
     */
    public Page<Message> getMessageHistory(Long roomId, Pageable pageable) {
        logger.debug("Retrieving message history for room ID: {} with pagination: {}", roomId, pageable);

        // Find chat room
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> {
                    logger.warn("Get message history failed: chat room not found: {}", roomId);
                    return new IllegalArgumentException("Chat room not found");
                });

        // Retrieve paginated messages
        Page<Message> messages = messageRepository.findByChatRoomOrderByTimestampAsc(chatRoom, pageable);
        logger.debug("Retrieved {} messages for room ID: {}", messages.getNumberOfElements(), roomId);

        return messages;
    }

    /**
     * Validates message content.
     *
     * @param content the message content to validate
     * @throws IllegalArgumentException if content is invalid
     */
    private void validateMessageContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }

        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Message content exceeds maximum length of " + MAX_MESSAGE_LENGTH);
        }
    }

    /**
     * Validates that a user is a member of a chat room.
     *
     * @param user     the user to validate
     * @param chatRoom the chat room to check membership in
     * @throws IllegalArgumentException if user is not a member
     */
    private void validateMembership(User user, ChatRoom chatRoom) {
        RoomMembership membership = roomMembershipRepository.findByUserAndChatRoom(user, chatRoom)
                .orElseThrow(() -> {
                    logger.warn("User ID: {} is not a member of room ID: {}", user.getId(), chatRoom.getId());
                    return new IllegalArgumentException("User is not a member of this chat room");
                });

        logger.debug("Membership validated for user ID: {} in room ID: {}", user.getId(), chatRoom.getId());
    }

    /**
     * Broadcasts a message to the STOMP topic for the chat room.
     *
     * @param message the message to broadcast
     */
    private void broadcastMessage(Message message) {
        String destination = "/topic/room/" + message.getChatRoom().getId();

        logger.debug("Broadcasting message ID: {} to topic: {}", message.getId(), destination);

        MessageResponse payload = MessageResponse.from(message);
        messagingTemplate.convertAndSend(destination, payload);

        logger.info("Message ID: {} successfully broadcast to topic: {}", message.getId(), destination);
    }
}
