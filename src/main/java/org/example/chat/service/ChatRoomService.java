package org.example.chat.service;

import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.MemberRole;
import org.example.chat.entity.RoomMembership;
import org.example.chat.entity.User;
import org.example.chat.exception.RoomNotFoundException;
import org.example.chat.exception.UnauthorizedException;
import org.example.chat.exception.UserNotFoundException;
import org.example.chat.repository.ChatRoomRepository;
import org.example.chat.repository.RoomMembershipRepository;
import org.example.chat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing chat rooms and room memberships.
 * Handles room creation, retrieval, membership management, and room queries.
 */
@Service
public class ChatRoomService {

    private static final Logger logger = LoggerFactory.getLogger(ChatRoomService.class);

    private final ChatRoomRepository chatRoomRepository;
    private final RoomMembershipRepository roomMembershipRepository;
    private final UserRepository userRepository;

    public ChatRoomService(ChatRoomRepository chatRoomRepository,
            RoomMembershipRepository roomMembershipRepository,
            UserRepository userRepository) {
        this.chatRoomRepository = chatRoomRepository;
        this.roomMembershipRepository = roomMembershipRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new chat room and adds the creator as OWNER.
     *
     * @param name        the name of the chat room (must be unique)
     * @param description optional description of the chat room
     * @param creatorId   the ID of the user creating the room
     * @return the created ChatRoom entity with creator as OWNER member
     * @throws IllegalArgumentException if name is invalid, already exists, or
     *                                  creator not found
     */
    @Transactional
    public ChatRoom createRoom(String name, String description, Long creatorId) {
        logger.info("Attempting to create chat room: {} by user ID: {}", name, creatorId);

        // Validate input
        validateRoomName(name);

        // Check if room name already exists
        if (chatRoomRepository.findByName(name).isPresent()) {
            logger.warn("Room creation failed: room name already exists: {}", name);
            throw new IllegalArgumentException("Room name already exists");
        }

        // Find creator user
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> {
                    logger.warn("Room creation failed: creator user not found: {}", creatorId);
                    return new IllegalArgumentException("Creator user not found");
                });

        // Create chat room
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setName(name);
        chatRoom.setDescription(description);
        chatRoom.setCreatedAt(LocalDateTime.now());
        chatRoom.setCreatedBy(creator);

        // Persist chat room
        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
        logger.info("Successfully created chat room: {} with ID: {}", name, savedRoom.getId());

        // Add creator as OWNER
        addMember(savedRoom.getId(), creatorId, MemberRole.OWNER);

        return savedRoom;
    }

    /**
     * Retrieves a chat room by its ID.
     *
     * @param roomId the ID of the chat room
     * @return the ChatRoom entity
     * @throws RoomNotFoundException if room is not found
     */
    public ChatRoom getRoomById(Long roomId) {
        logger.debug("Retrieving chat room by ID: {}", roomId);
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> {
                    logger.warn("Chat room not found: {}", roomId);
                    return new RoomNotFoundException(roomId);
                });
    }

    /**
     * Returns all available chat rooms.
     *
     * @return list of all ChatRoom entities
     */
    public List<ChatRoom> listRooms() {
        logger.debug("Retrieving all chat rooms");
        return chatRoomRepository.findAll();
    }

    /**
     * Returns all users who are members of a specific chat room.
     *
     * @param roomId the ID of the chat room
     * @return list of User entities who are members of the room
     * @throws IllegalArgumentException if room is not found
     */
    public List<User> getRoomMembers(Long roomId) {
        logger.debug("Retrieving members for chat room ID: {}", roomId);

        // Verify room exists
        ChatRoom room = getRoomById(roomId);

        // Get all memberships for the room
        List<RoomMembership> memberships = roomMembershipRepository.findByChatRoom(room);

        // Extract users from memberships
        List<User> members = memberships.stream()
                .map(RoomMembership::getUser)
                .collect(Collectors.toList());

        logger.debug("Found {} members for chat room ID: {}", members.size(), roomId);
        return members;
    }

    /**
     * Adds a user as a member to a chat room with the specified role.
     *
     * @param roomId the ID of the chat room
     * @param userId the ID of the user to add
     * @param role   the role to assign to the user (defaults to MEMBER if null)
     * @return the created RoomMembership entity
     * @throws IllegalArgumentException if room or user not found, or if user is
     *                                  already a member
     */
    @Transactional
    public RoomMembership addMember(Long roomId, Long userId, MemberRole role) {
        logger.info("Adding user ID: {} to chat room ID: {} with role: {}", userId, roomId, role);

        // Find room and user
        ChatRoom room = getRoomById(roomId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("Add member failed: user not found: {}", userId);
                    return new IllegalArgumentException("User not found");
                });

        // Return existing membership when user is already a member
        RoomMembership existingMembership = roomMembershipRepository.findByUserAndChatRoom(user, room)
                .orElse(null);
        if (existingMembership != null) {
            logger.debug("User {} is already a member of room {}, returning existing membership", userId, roomId);
            return existingMembership;
        }

        // Create membership
        RoomMembership membership = new RoomMembership();
        membership.setUser(user);
        membership.setChatRoom(room);
        membership.setJoinedAt(LocalDateTime.now());
        membership.setRole(role != null ? role : MemberRole.MEMBER);

        // Persist membership
        RoomMembership savedMembership = roomMembershipRepository.save(membership);
        logger.info("Successfully added user ID: {} to chat room ID: {}", userId, roomId);

        return savedMembership;
    }

    /**
     * Removes a user from a chat room by deleting their membership.
     *
     * @param roomId the ID of the chat room
     * @param userId the ID of the user to remove
     * @throws IllegalArgumentException if room or user not found, or if user is not
     *                                  a member
     */
    @Transactional
    public void removeMember(Long roomId, Long userId) {
        logger.info("Removing user ID: {} from chat room ID: {}", userId, roomId);

        // Find room and user
        ChatRoom room = getRoomById(roomId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("Remove member failed: user not found: {}", userId);
                    return new IllegalArgumentException("User not found");
                });

        // Check if user is a member
        if (roomMembershipRepository.findByUserAndChatRoom(user, room).isEmpty()) {
            logger.warn("Remove member failed: user {} is not a member of room {}", userId, roomId);
            throw new IllegalArgumentException("User is not a member of this room");
        }

        // Delete membership
        roomMembershipRepository.deleteByUserAndChatRoom(user, room);
        logger.info("Successfully removed user ID: {} from chat room ID: {}", userId, roomId);
    }

    /**
     * Deletes a chat room if the user has OWNER or MODERATOR role.
     *
     * @param roomId the ID of the chat room
     * @param userId the ID of the user attempting deletion
     */
    @Transactional
    public void deleteRoom(Long roomId, Long userId) {
        logger.info("Deleting chat room ID: {} by user ID: {}", roomId, userId);

        ChatRoom room = getRoomById(roomId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        RoomMembership membership = roomMembershipRepository.findByUserAndChatRoom(user, room)
                .orElseThrow(() -> new UnauthorizedException("User is not a member of this room"));

        if (membership.getRole() != MemberRole.OWNER && membership.getRole() != MemberRole.MODERATOR) {
            throw new UnauthorizedException("Only owners or moderators can delete rooms");
        }

        chatRoomRepository.delete(room);
        logger.info("Deleted chat room ID: {}", roomId);
    }

    /**
     * Validates room name input.
     *
     * @param name the room name to validate
     * @throws IllegalArgumentException if name is invalid
     */
    private void validateRoomName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Room name cannot be empty");
        }

        if (name.length() > 100) {
            throw new IllegalArgumentException("Room name cannot exceed 100 characters");
        }
    }
}
