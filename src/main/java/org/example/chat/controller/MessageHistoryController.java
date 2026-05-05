package org.example.chat.controller;

import org.example.chat.dto.MessageResponse;
import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.Message;
import org.example.chat.entity.RoomMembership;
import org.example.chat.entity.User;
import org.example.chat.exception.RoomNotFoundException;
import org.example.chat.repository.ChatRoomRepository;
import org.example.chat.repository.RoomMembershipRepository;
import org.example.chat.repository.UserRepository;
import org.example.chat.service.ChatMessageService;
import org.example.chat.service.ChatRoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for message history operations.
 * Provides HTTP access to paginated message history with membership validation.
 */
@RestController
@RequestMapping("/api/rooms")
public class MessageHistoryController {

        private static final Logger logger = LoggerFactory.getLogger(MessageHistoryController.class);

        private final ChatMessageService chatMessageService;
        private final ChatRoomService chatRoomService;
        private final ChatRoomRepository chatRoomRepository;
        private final RoomMembershipRepository roomMembershipRepository;
        private final UserRepository userRepository;

        public MessageHistoryController(ChatMessageService chatMessageService,
                        ChatRoomService chatRoomService,
                        ChatRoomRepository chatRoomRepository,
                        RoomMembershipRepository roomMembershipRepository,
                        UserRepository userRepository) {
                this.chatMessageService = chatMessageService;
                this.chatRoomService = chatRoomService;
                this.chatRoomRepository = chatRoomRepository;
                this.roomMembershipRepository = roomMembershipRepository;
                this.userRepository = userRepository;
        }

        /**
         * Retrieves paginated message history for a chat room.
         * Validates that the requesting user is a member of the room before returning
         * messages.
         *
         * @param roomId      the ID of the chat room
         * @param pageable    pagination parameters (page, size, sort)
         * @param userDetails the authenticated user making the request
         * @return ResponseEntity with Page of MessageResponse
         * @throws IllegalArgumentException if room not found or user is not a member
         */
        @GetMapping("/{roomId}/messages")
        public ResponseEntity<Page<MessageResponse>> getMessageHistory(
                        @PathVariable Long roomId,
                        Pageable pageable,
                        @AuthenticationPrincipal UserDetails userDetails) {

                logger.info("Message history request for room ID: {} by user: {}",
                                roomId, userDetails.getUsername());

                try {
                        // Get the authenticated user
                        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "Authenticated user not found"));

                        // Get the chat room
                        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                                        .orElseThrow(() -> {
                                                logger.warn("Message history request failed: chat room not found: {}",
                                                                roomId);
                                                return new RoomNotFoundException(roomId);
                                        });

                        // Ensure the user is a member of the room (auto-join if needed)
                        RoomMembership membership = roomMembershipRepository
                                        .findByUserAndChatRoom(currentUser, chatRoom)
                                        .orElseGet(() -> chatRoomService.addMember(roomId, currentUser.getId(), null));

                        logger.debug("Membership validated for user ID: {} in room ID: {}",
                                        currentUser.getId(), roomId);

                        // Retrieve paginated message history
                        Page<Message> messages = chatMessageService.getMessageHistory(roomId, pageable);

                        // Convert to DTOs and return paged response
                        Page<MessageResponse> response = messages.map(MessageResponse::from);

                        logger.info("Retrieved {} messages for room ID: {}",
                                        response.getNumberOfElements(), roomId);

                        return ResponseEntity.ok(response);

                } catch (IllegalArgumentException e) {
                        logger.warn("Message history request failed for room {}: {}", roomId, e.getMessage());
                        throw e;
                }
        }
}
