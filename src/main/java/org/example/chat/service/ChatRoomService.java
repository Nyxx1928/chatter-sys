package org.example.chat.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.MemberRole;
import org.example.chat.entity.RoomMembership;
import org.example.chat.entity.RoomType;
import org.example.chat.entity.User;
import org.example.chat.exception.RoomNotFoundException;
import org.example.chat.exception.UnauthorizedException;
import org.example.chat.exception.UserNotFoundException;
import org.example.chat.repository.ChatRoomRepository;
import org.example.chat.repository.MessageRepository;
import org.example.chat.repository.RoomMembershipRepository;
import org.example.chat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing chat rooms and room memberships.
 * Handles room creation, retrieval, membership management, and room queries.
 */
@Service
public class ChatRoomService {

    private static final Logger logger = LoggerFactory.getLogger(ChatRoomService.class);

    @PersistenceContext
    private EntityManager entityManager; // NOSONAR: used to clear session on concurrent write conflicts

    private final ChatRoomRepository chatRoomRepository;
    private final RoomMembershipRepository roomMembershipRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final RateLimiterService rateLimiterService;

    public ChatRoomService(ChatRoomRepository chatRoomRepository,
            RoomMembershipRepository roomMembershipRepository,
            UserRepository userRepository,
            MessageRepository messageRepository,
            RateLimiterService rateLimiterService) {
        this.chatRoomRepository = chatRoomRepository;
        this.roomMembershipRepository = roomMembershipRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.rateLimiterService = rateLimiterService;
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

        // Rate-limit room creation per user
        rateLimiterService.checkRoomCreation(creatorId);

        // Validate input
        validateRoomName(name);

        // Check if room name already exists
        if (chatRoomRepository.findByNameAndRoomType(name, RoomType.GROUP).isPresent()) {
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

        // Persist chat room. The DB unique constraint (name, room_type) is the
        // backstop for the check above when two concurrent creates race.
        ChatRoom savedRoom;
        try {
            savedRoom = chatRoomRepository.save(chatRoom);
        } catch (DataIntegrityViolationException e) {
            logger.warn("Room creation failed: room name already exists (concurrent): {}", name);
            throw new IllegalArgumentException("Room name already exists");
        }
        logger.info("Successfully created chat room: {} with ID: {}", name, savedRoom.getId());

        // Add creator as OWNER (requesterId null = internal creation call)
        addMember(savedRoom.getId(), creatorId, MemberRole.OWNER, null);

        return savedRoom;
    }

    /**
     * Retrieves a chat room by its ID.
     *
     * @param roomId the ID of the chat room
     * @return the ChatRoom entity
     * @throws RoomNotFoundException if room is not found
     */
    @Transactional(readOnly = true)
    public ChatRoom getRoomById(Long roomId) {
        logger.debug("Retrieving chat room by ID: {}", roomId);
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> {
                    logger.warn("Chat room not found: {}", roomId);
                    return new RoomNotFoundException(roomId);
                });
    }

    /**
     * Returns all GROUP chat rooms.
     * DIRECT rooms are intentionally excluded to avoid leaking private
     * conversations to users who are not participants.
     *
     * @return list of GROUP ChatRoom entities
     */
    @Transactional(readOnly = true)
    public List<ChatRoom> listRooms() {
        logger.debug("Retrieving all group chat rooms");
        return chatRoomRepository.findByRoomType(RoomType.GROUP);
    }

    /**
     * Returns only the rooms that the given user is a member of.
     *
     * @param user the user whose rooms to retrieve
     * @return list of ChatRoom entities the user has joined
     */
    @Transactional(readOnly = true)
    public List<ChatRoom> listRoomsForUser(User user) {
        logger.debug("Retrieving rooms for user: {}", user.getUsername());
        return chatRoomRepository.findByMembersContaining(user);
    }

    /**
     * Returns all users who are members of a specific chat room.
     *
     * @param roomId the ID of the chat room
     * @return list of User entities who are members of the room
     * @throws IllegalArgumentException if room is not found
     */
    @Transactional(readOnly = true)
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
     * Idempotent — returns the existing membership if the user is already a member.
     * Throws UnauthorizedException if the room is a DIRECT room.
     *
     * Authorization: when {@code requesterId} is provided the requester must
     * already be a member and may only add plain MEMBERs (prevents privilege
     * escalation). Pass {@code requesterId = null} only for trusted internal
     * calls such as room creation.
     *
     * @param roomId the ID of the chat room
     * @param userId the ID of the user to add
     * @param role   the role to assign to the user (defaults to MEMBER if null)
     * @param requesterId the ID of the user performing the operation, or null for internal calls
     * @return the created or existing RoomMembership entity
     */
    @Transactional
    public RoomMembership addMember(Long roomId, Long userId, MemberRole role, Long requesterId) {
        logger.info("Adding user ID: {} to chat room ID: {} with role: {} by requester: {}",
                userId, roomId, role, requesterId);

        // Find room and user
        ChatRoom room = chatRoomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> {
                    logger.warn("Add member failed: room not found: {}", roomId);
                    return new IllegalArgumentException("Chat room not found");
                });

        // Guard: DM rooms cannot have members added manually
        if (room.getRoomType() == RoomType.DIRECT) {
            throw new UnauthorizedException("Cannot invite users to a direct message room");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("Add member failed: user not found: {}", userId);
                    return new IllegalArgumentException("User not found");
                });

        // Authorization: external callers must be members and can only grant MEMBER role
        if (requesterId != null) {
            User requester = userRepository.findById(requesterId)
                    .orElseThrow(() -> {
                        logger.warn("Add member failed: requester not found: {}", requesterId);
                        return new UserNotFoundException(requesterId);
                    });
            RoomMembership requesterMembership = roomMembershipRepository
                    .findByUserAndChatRoom(requester, room)
                    .orElseThrow(() -> new UnauthorizedException(
                            "Only members of the room can invite users"));
            if (requesterMembership.getRole() == null) {
                logger.warn("Add member failed: requester {} has no role in room {}", requesterId, roomId);
                throw new UnauthorizedException("Only members of the room can invite users");
            }
            if (role != null && role != MemberRole.MEMBER) {
                logger.warn("Add member blocked: requester {} attempted to grant role {} in room {}",
                        requesterId, role, roomId);
                throw new UnauthorizedException("Only room owners can assign elevated roles");
            }
        }

        // Check existing membership first (optimistic path)
        Optional<RoomMembership> existing = roomMembershipRepository.findByUserAndChatRoom(user, room);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Try to create membership, handling concurrent duplicate inserts
        try {
            RoomMembership membership = new RoomMembership();
            membership.setUser(user);
            membership.setChatRoom(room);
            membership.setJoinedAt(LocalDateTime.now());
            membership.setRole(role != null ? role : MemberRole.MEMBER);
            RoomMembership saved = roomMembershipRepository.save(membership);
            logger.info("Successfully added user ID: {} to chat room ID: {}", userId, roomId);
            return saved;
        } catch (DataIntegrityViolationException e) {
            // Concurrent duplicate insert — another thread created the membership first.
            // Clear the EntityManager to reset session state after the failed insert.
            entityManager.clear();
            logger.warn("Concurrent addMember detected for user {} in room {}, fetching existing", userId, roomId);
            return roomMembershipRepository.findByUserAndChatRoom(user, room)
                    .orElseThrow(() -> new IllegalStateException(
                            "Membership creation failed due to concurrent access", e));
        }
    }

    /**
     * Removes a user from a chat room by deleting their membership.
     *
     * Authorization rules:
     * <ul>
     *   <li>OWNER may remove anyone.</li>
     *   <li>MODERATOR may remove MEMBERs only.</li>
     *   <li>Any member may remove themselves (leave the room).</li>
     *   <li>The last remaining OWNER cannot be removed.</li>
     * </ul>
     *
     * @param roomId the ID of the chat room
     * @param userId the ID of the user to remove
     * @param requesterId the ID of the user performing the removal
     * @throws IllegalArgumentException if room or user not found, or if user is not
     *                                  a member
     * @throws UnauthorizedException if the requester lacks permission
     */
    @Transactional
    public void removeMember(Long roomId, Long userId, Long requesterId) {
        logger.info("Removing user ID: {} from chat room ID: {} by requester: {}", userId, roomId, requesterId);

        // Find room and target user
        ChatRoom room = chatRoomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> {
                    logger.warn("Remove member failed: room not found: {}", roomId);
                    return new RoomNotFoundException(roomId);
                });
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("Remove member failed: user not found: {}", userId);
                    return new IllegalArgumentException("User not found");
                });

        // Requester must be a member of the room
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new UserNotFoundException(requesterId));
        RoomMembership requesterMembership = roomMembershipRepository
                .findByUserAndChatRoom(requester, room)
                .orElseThrow(() -> new UnauthorizedException("Only members can remove users from this room"));

        // Target must be a member
        RoomMembership targetMembership = roomMembershipRepository.findByUserAndChatRoom(user, room)
                .orElseThrow(() -> {
                    logger.warn("Remove member failed: user {} is not a member of room {}", userId, roomId);
                    return new IllegalArgumentException("User is not a member of this room");
                });

        MemberRole requesterRole = requesterMembership.getRole();
        boolean selfRemoval = requesterId.equals(userId);
        if (requesterRole != MemberRole.OWNER && !selfRemoval) {
            if (requesterRole != MemberRole.MODERATOR || targetMembership.getRole() != MemberRole.MEMBER) {
                logger.warn("Remove member blocked: requester {} (role {}) cannot remove user {} (role {}) from room {}",
                        requesterId, requesterRole, userId, targetMembership.getRole(), roomId);
                throw new UnauthorizedException("You do not have permission to remove this member");
            }
        }

        // Never remove the last OWNER — the room would be left without an owner
        if (targetMembership.getRole() == MemberRole.OWNER
                && roomMembershipRepository.countByChatRoomAndRole(room, MemberRole.OWNER) <= 1) {
            logger.warn("Remove member blocked: user {} is the last owner of room {}", userId, roomId);
            throw new UnauthorizedException("Cannot remove the last owner of the room");
        }

        // Delete membership
        roomMembershipRepository.deleteByUserAndChatRoom(user, room);
        logger.info("Successfully removed user ID: {} from chat room ID: {}", userId, roomId);
    }

    /**
     * Deletes a chat room if the user has OWNER or MODERATOR role.
     * Throws UnauthorizedException if the room is a DIRECT room.
     *
     * @param roomId the ID of the chat room
     * @param userId the ID of the user attempting deletion
     */
    @Transactional
    public void deleteRoom(Long roomId, Long userId) {
        logger.info("Deleting chat room ID: {} by user ID: {}", roomId, userId);

        ChatRoom room = getRoomById(roomId);

        // Guard: DM rooms cannot be deleted
        if (room.getRoomType() == RoomType.DIRECT) {
            throw new UnauthorizedException("Cannot delete a direct message room");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        RoomMembership membership = roomMembershipRepository.findByUserAndChatRoom(user, room)
                .orElseThrow(() -> new UnauthorizedException("User is not a member of this room"));

        if (membership.getRole() != MemberRole.OWNER && membership.getRole() != MemberRole.MODERATOR) {
            throw new UnauthorizedException("Only owners or moderators can delete rooms");
        }

        // Bulk-delete children first to avoid JPA cascade loading every row
        // into memory (N+1 deletes) for large rooms.
        messageRepository.deleteByChatRoom(room);
        roomMembershipRepository.deleteByChatRoom(room);
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
