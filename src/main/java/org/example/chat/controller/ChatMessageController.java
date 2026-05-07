package org.example.chat.controller;

import org.example.chat.dto.MessageResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final SimpMessagingTemplate messagingTemplate;

    public ChatMessageController(ChatMessageService chatMessageService,
            ChatRoomService chatRoomService,
            UserRepository userRepository,
            RoomMembershipRepository roomMembershipRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.chatMessageService = chatMessageService;
        this.chatRoomService = chatRoomService;
        this.userRepository = userRepository;
        this.roomMembershipRepository = roomMembershipRepository;
        this.messagingTemplate = messagingTemplate;
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
        logger.info("Received message from user: {} for room: {}", principal.getName(), roomId);

        // Extract authenticated user
        User sender = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + principal.getName()));

        // Delegate to service to validate, persist, and broadcast
        chatMessageService.sendMessage(sender.getId(), roomId, message.getContent());

        logger.info("Message sent successfully by user: {} to room: {}", sender.getUsername(), roomId);
    }

    /**
     * Handles room join requests from clients.
     * 
     * Adds the user to the room membership and broadcasts a JOIN system message
     * to all room subscribers.
     * 
     * @param roomId    the ID of the chat room to join
     * @param principal the authenticated user principal
     */
    @MessageMapping("/room.join/{roomId}")
    public void joinRoom(@DestinationVariable Long roomId, Principal principal) {
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

        // Create and broadcast JOIN system message
        Message joinMessage = new Message();
        joinMessage.setSender(user);
        joinMessage.setChatRoom(chatRoomService.getRoomById(roomId));
        joinMessage.setContent(user.getDisplayName() + " joined the room");
        joinMessage.setTimestamp(LocalDateTime.now());
        joinMessage.setMessageType(MessageType.JOIN);

        // Broadcast to room topic
        String destination = "/topic/room/" + roomId;
        messagingTemplate.convertAndSend(destination, MessageResponse.from(joinMessage));

        logger.info("User: {} successfully joined room: {}", user.getUsername(), roomId);
    }

    /**
     * Handles room leave requests from clients.
     * 
     * Broadcasts a LEAVE system message to all remaining room subscribers.
     * Note: membership is intentionally preserved so users can re-enter the room
     * without needing to re-join. Navigation away from a room should not
     * permanently remove membership.
     * 
     * @param roomId    the ID of the chat room to leave
     * @param principal the authenticated user principal
     */
    @MessageMapping("/room.leave/{roomId}")
    public void leaveRoom(@DestinationVariable Long roomId, Principal principal) {
        logger.info("User: {} leaving room: {}", principal.getName(), roomId);

        // Extract authenticated user
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + principal.getName()));

        // Create and broadcast LEAVE system message (membership is preserved)
        Message leaveMessage = new Message();
        leaveMessage.setSender(user);
        leaveMessage.setChatRoom(chatRoomService.getRoomById(roomId));
        leaveMessage.setContent(user.getDisplayName() + " left the room");
        leaveMessage.setTimestamp(LocalDateTime.now());
        leaveMessage.setMessageType(MessageType.LEAVE);

        // Broadcast to room topic
        String destination = "/topic/room/" + roomId;
        messagingTemplate.convertAndSend(destination, MessageResponse.from(leaveMessage));

        logger.info("User: {} left room: {} (membership preserved)", user.getUsername(), roomId);
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
}
