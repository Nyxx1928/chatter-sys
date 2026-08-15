package org.example.chat.controller;

import jakarta.validation.Valid;
import org.example.chat.dto.ChatRoomResponse;
import org.example.chat.dto.CreateRoomRequest;
import org.example.chat.dto.UserResponse;
import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.RoomType;
import org.example.chat.entity.User;
import org.example.chat.entity.MemberRole;
import org.example.chat.exception.RoomNotFoundException;
import org.example.chat.exception.UnauthorizedException;
import org.example.chat.repository.RoomMembershipRepository;
import org.example.chat.repository.UserRepository;
import org.example.chat.service.ChatRoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for chat room operations.
 * Handles room creation, retrieval, and member management.
 */
@RestController
@RequestMapping("/api/rooms")
public class ChatRoomController {

    private static final Logger logger = LoggerFactory.getLogger(ChatRoomController.class);

    private final ChatRoomService chatRoomService;
    private final UserRepository userRepository;
    private final RoomMembershipRepository roomMembershipRepository;

    public ChatRoomController(ChatRoomService chatRoomService,
            UserRepository userRepository,
            RoomMembershipRepository roomMembershipRepository) {
        this.chatRoomService = chatRoomService;
        this.userRepository = userRepository;
        this.roomMembershipRepository = roomMembershipRepository;
    }

    /**
     * Creates a new chat room.
     *
     * @param request     the room creation request containing name and optional
     *                    description
     * @param userDetails the authenticated user creating the room
     * @return ResponseEntity with ChatRoomResponse and HTTP 201 Created status
     */
    @PostMapping
    public ResponseEntity<ChatRoomResponse> createRoom(
            @Valid @RequestBody CreateRoomRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        logger.info("Room creation request received: {} by user: {}",
                request.getName(), userDetails.getUsername());

        try {
            // Get the User entity from the authenticated username
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

            ChatRoom chatRoom = chatRoomService.createRoom(
                    request.getName(),
                    request.getDescription(),
                    currentUser.getId());

            ChatRoomResponse response = ChatRoomResponse.from(chatRoom);
            logger.info("Room created successfully: {} with ID: {}",
                    request.getName(), chatRoom.getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Room creation failed for {}: {}",
                    request.getName(), e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieves only the chat rooms the authenticated user is a member of.
     *
     * @param userDetails the authenticated user
     * @return ResponseEntity with list of ChatRoomResponse
     */
    @GetMapping
    public ResponseEntity<List<ChatRoomResponse>> listRooms(
            @AuthenticationPrincipal UserDetails userDetails) {
        logger.debug("Retrieving rooms for user: {}", userDetails.getUsername());

        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        List<ChatRoom> rooms = chatRoomService.listRoomsForUser(currentUser);
        List<ChatRoomResponse> response = rooms.stream()
                .map(ChatRoomResponse::from)
                .collect(Collectors.toList());

        logger.debug("Retrieved {} rooms for user: {}", response.size(), userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Invites a user to join a room. Only existing members can invite.
     *
     * @param id          the ID of the room
     * @param inviteeId   the ID of the user to invite
     * @param userDetails the authenticated user (inviter)
     * @return ResponseEntity with the created membership
     */
    @PostMapping("/{id}/invite")
    public ResponseEntity<Void> inviteToRoom(
            @PathVariable Long id,
            @RequestParam Long inviteeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        logger.info("Invite request: room ID {} inviting user ID {} by user: {}",
                id, inviteeId, userDetails.getUsername());

        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        // Verify the inviter is a member of the room
        ChatRoom chatRoom = chatRoomService.getRoomById(id);
        roomMembershipRepository.findByUserAndChatRoom(currentUser, chatRoom)
                .orElseThrow(() -> new UnauthorizedException("You are not a member of this room"));

        // Guard: DM rooms cannot be invited to
        if (chatRoom.getRoomType() == RoomType.DIRECT) {
            throw new UnauthorizedException("Cannot invite users to a direct message room");
        }

        // Add the invitee as a MEMBER
        chatRoomService.addMember(id, inviteeId, MemberRole.MEMBER, currentUser.getId());

        logger.info("User ID {} successfully invited to room ID {} by user: {}",
                inviteeId, id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves details of a specific chat room.
     *
     * @param id          the ID of the chat room
     * @param userDetails the authenticated user making the request
     * @return ResponseEntity with ChatRoomResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChatRoomResponse> getRoomById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        logger.debug("Retrieving chat room with ID: {}", id);

        try {
            // Get the authenticated user
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

            // Get the chat room
            ChatRoom chatRoom = chatRoomService.getRoomById(id);

            // Enforce membership — user must have explicitly joined
            roomMembershipRepository.findByUserAndChatRoom(currentUser, chatRoom)
                    .orElseThrow(() -> new UnauthorizedException("You are not a member of this room"));

            ChatRoomResponse response = ChatRoomResponse.from(chatRoom);
            logger.debug("Retrieved chat room: {}", chatRoom.getName());
            return ResponseEntity.ok(response);
        } catch (RoomNotFoundException e) {
            logger.warn("Chat room not found: {}", id);
            throw e;
        } catch (UnauthorizedException e) {
            logger.warn("Unauthorized access to chat room: {}", id);
            throw e;
        }
    }

    /**
     * Retrieves all members of a specific chat room.
     * Only existing members of the room can view the member list.
     *
     * @param id the ID of the chat room
     * @param userDetails the authenticated user making the request
     * @return ResponseEntity with list of UserResponse
     */
    @GetMapping("/{id}/members")
    public ResponseEntity<List<UserResponse>> getRoomMembers(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        logger.debug("Retrieving members for chat room ID: {}", id);

        try {
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

            ChatRoom chatRoom = chatRoomService.getRoomById(id);

            // Enforce membership — only members can see who else is in the room
            roomMembershipRepository.findByUserAndChatRoom(currentUser, chatRoom)
                    .orElseThrow(() -> new UnauthorizedException("You are not a member of this room"));

            List<User> members = chatRoomService.getRoomMembers(id);
            List<UserResponse> response = members.stream()
                    .map(UserResponse::from)
                    .collect(Collectors.toList());

            logger.debug("Retrieved {} members for chat room ID: {}", response.size(), id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to retrieve members for chat room {}: {}", id, e.getMessage());
            throw e;
        }
    }

    /**
     * Deletes a chat room if the user is an owner or moderator.
     *
     * @param id the ID of the chat room
     * @param userDetails the authenticated user making the request
     * @return ResponseEntity with HTTP 204 No Content status
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        logger.info("Room deletion request for room ID: {} by user: {}", id, userDetails.getUsername());

        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        chatRoomService.deleteRoom(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
