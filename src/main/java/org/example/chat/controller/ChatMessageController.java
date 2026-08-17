package org.example.chat.controller;

import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.Message;
import org.example.chat.entity.MessageType;
import org.example.chat.entity.User;
import org.example.chat.exception.ErrorResponse;
import org.example.chat.exception.UnauthorizedException;
import org.example.chat.repository.RoomMembershipRepository;
import org.example.chat.repository.UserRepository;
import org.example.chat.service.ChatMessageService;
import org.example.chat.service.ChatRoomService;
import org.example.chat.util.SecurityAuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;

/**
 * STOMP message controller for handling real-time chat operations.
 * 
 * This controller handles WebSocket STOMP messages for:
 * - Sending chat messages
 * - Joining chat rooms
 * - Leaving chat rooms
 * 
 * Requirements: 3.1, 3.2, 5.1, 5.3, 9.3
 */
@Controller
public class ChatMessageController {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageController.class);

    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;
    private final UserRepository userRepository;
    private final RoomMembershipRepository roomMembershipRepository;
    private final SecurityAuditLogger securityAuditLogger;

    public ChatMessageController(ChatMessageService chatMessageService,
            ChatRoomService chatRoomService,
            UserRepository userRepository,
            RoomMembershipRepository roomMembershipRepository,
            SecurityAuditLogger securityAuditLogger) {
        this.chatMessageService = chatMessageService;
        this.chatRoomService = chatRoomService;
        this.userRepository = userRepository;
        this.roomMembershipRepository = roomMembershipRepository;
        this.securityAuditLogger = securityAuditLogger;
    }

    /**
     * Handles incoming chat messages from clients.
     * 
     * Receives a message from a STOMP client, validates it, persists it,
     * and broadcasts it to all subscribers of the room topic.
     * 
     * @param message   the message to send
     * @param roomId    the ID of the chat room
     * @param principal the authenticated user principal
     */
    @MessageMapping("/chat.send/{roomId}")
    public void sendMessage(@Payload Message message,
            @DestinationVariable Long roomId,
            Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new UnauthorizedException("Authentication required");
        }
        logger.info("Received message from user: {} for room: {}", principal.getName(), roomId);

        // Extract authenticated user
        User sender = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + principal.getName()));

        // NEW: Verify user is a member of the room BEFORE processing
        ChatRoom chatRoom = chatRoomService.getRoomById(roomId);
        if (roomMembershipRepository.findByUserAndChatRoom(sender, chatRoom).isEmpty()) {
            logger.warn("Unauthorized message attempt: user {} to room {}", sender.getId(), roomId);
            securityAuditLogger.logAuthorizationFailure(sender.getId(), roomId, 
                "User attempted to send message to room they are not a member of");
            throw new UnauthorizedException("User is not a member of this room");
        }

        // Delegate to service to validate, persist, and broadcast
        chatMessageService.sendMessage(sender.getId(), roomId, message.getContent());

        logger.info("Message sent successfully by user: {} to room: {}", sender.getUsername(), roomId);
    }

    /**
     * Handles room join requests from clients.
     * 
     * Persists and broadcasts a JOIN system message so it also shows up in the
     * room's message history.
     * 
     * @param roomId    the ID of the chat room to join
     * @param principal the authenticated user principal
     */
    @MessageMapping("/room.join/{roomId}")
    public void joinRoom(@DestinationVariable Long roomId, Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new UnauthorizedException("Authentication required");
        }
        logger.info("User: {} joining room: {}", principal.getName(), roomId);

        // Extract authenticated user
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + principal.getName()));

        // Verify the user is already a member — joining via STOMP is only for
        // re-announcing presence, not for gaining access to a room
        ChatRoom chatRoom = chatRoomService.getRoomById(roomId);
        roomMembershipRepository.findByUserAndChatRoom(user, chatRoom)
                .orElseThrow(() -> new UnauthorizedException(
                        "You are not a member of this room"));

        chatMessageService.sendSystemMessage(user.getId(), roomId, MessageType.JOIN,
                userDisplayName(user) + " joined the room");

        logger.info("User: {} successfully joined room: {}", user.getUsername(), roomId);
    }

    /**
     * Handles room leave requests from clients.
     * 
     * Persists and broadcasts a LEAVE system message so it also shows up in the
     * room's message history.
     * Note: membership is intentionally preserved so users can re-enter the room
     * without needing to re-join. Navigation away from a room should not
     * permanently remove membership.
     * 
     * @param roomId    the ID of the chat room to leave
     * @param principal the authenticated user principal
     */
    @MessageMapping("/room.leave/{roomId}")
    public void leaveRoom(@DestinationVariable Long roomId, Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new UnauthorizedException("Authentication required");
        }
        logger.info("User: {} leaving room: {}", principal.getName(), roomId);

        // Extract authenticated user
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + principal.getName()));

        // Enforce membership before broadcasting LEAVE system messages
        ChatRoom chatRoom = chatRoomService.getRoomById(roomId);
        roomMembershipRepository.findByUserAndChatRoom(user, chatRoom)
                .orElseThrow(() -> new UnauthorizedException("You are not a member of this room"));

        chatMessageService.sendSystemMessage(user.getId(), roomId, MessageType.LEAVE,
                userDisplayName(user) + " left the room");

        logger.info("User: {} left room: {} (membership preserved)", user.getUsername(), roomId);
    }

    /**
     * Returns the user's display name, falling back to the username when no
     * display name has been set.
     */
    private String userDisplayName(User user) {
        return user.getDisplayName() != null && !user.getDisplayName().isBlank()
                ? user.getDisplayName()
                : user.getUsername();
    }

    /**
     * Exception handler specifically for validation failures.
     * 
     * Catches IllegalArgumentException (e.g. empty or oversized messages) and
     * sends a 400 error response to the user's error queue.
     * 
     * @param exception the IllegalArgumentException that occurred
     * @param principal the authenticated user principal
     * @return error response sent to user's error queue
     */
    @MessageExceptionHandler(IllegalArgumentException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleValidationException(IllegalArgumentException exception, Principal principal) {
        logger.warn("Validation error for user: {}: {}",
                principal != null ? principal.getName() : "unknown", exception.getMessage());

        return new ErrorResponse(
                exception.getMessage(),
                LocalDateTime.now(),
                400);
    }

    /**
     * Exception handler for STOMP message processing errors.
     * 
     * Catches exceptions thrown during message handling and sends
     * error messages to the user's error queue.
     * 
     * @param exception the exception that occurred
     * @param principal the authenticated user principal
     * @return error response sent to user's error queue
     */
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public ErrorResponse handleException(Exception exception, Principal principal) {
        logger.error("Error processing STOMP message for user: {}",
                principal != null ? principal.getName() : "unknown", exception);

        ErrorResponse errorResponse = new ErrorResponse(
                exception.getMessage(),
                LocalDateTime.now(),
                500);

        return errorResponse;
    }

    /**
     * Exception handler specifically for authorization failures.
     * 
     * Catches UnauthorizedException and sends a 403 error response
     * to the user's error queue.
     * 
     * @param exception the UnauthorizedException that occurred
     * @param principal the authenticated user principal
     * @return error response sent to user's error queue
     */
    @MessageExceptionHandler(UnauthorizedException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleAuthorizationException(UnauthorizedException exception, Principal principal) {
        logger.warn("Authorization error for user: {}: {}", 
                principal != null ? principal.getName() : "unknown", exception.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                exception.getMessage(),
                LocalDateTime.now(),
                403);

        return errorResponse;
    }
}
