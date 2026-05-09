package org.example.chat.service;

import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.MemberRole;
import org.example.chat.entity.RoomMembership;
import org.example.chat.entity.RoomType;
import org.example.chat.entity.User;
import org.example.chat.repository.ChatRoomRepository;
import org.example.chat.repository.RoomMembershipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for managing direct message (DM) rooms between two users.
 * DM rooms are created automatically when a friendship is accepted and are
 * identified by a deterministic name based on the two user IDs.
 */
@Service
public class DirectMessageService {

    private static final Logger logger = LoggerFactory.getLogger(DirectMessageService.class);

    private final ChatRoomRepository chatRoomRepository;
    private final RoomMembershipRepository roomMembershipRepository;

    public DirectMessageService(ChatRoomRepository chatRoomRepository,
                                RoomMembershipRepository roomMembershipRepository) {
        this.chatRoomRepository = chatRoomRepository;
        this.roomMembershipRepository = roomMembershipRepository;
    }

    /**
     * Returns the DM room between two users, creating it (with both memberships)
     * if it does not yet exist. The operation is idempotent — calling it multiple
     * times with the same pair always returns the same room.
     *
     * @param userA one participant
     * @param userB the other participant
     * @return the existing or newly created DM ChatRoom
     */
    @Transactional
    public ChatRoom getOrCreateDmRoom(User userA, User userB) {
        String dmName = buildDmName(userA, userB);
        logger.info("getOrCreateDmRoom: looking up DM room '{}' for users {} and {}",
                dmName, userA.getId(), userB.getId());

        return chatRoomRepository.findByNameAndRoomType(dmName, RoomType.DIRECT)
                .orElseGet(() -> {
                    logger.info("DM room '{}' not found — creating new room", dmName);

                    ChatRoom room = new ChatRoom();
                    room.setName(dmName);
                    room.setRoomType(RoomType.DIRECT);
                    room.setCreatedAt(LocalDateTime.now());
                    // DM rooms have no single "owner" — createdBy is left null
                    ChatRoom saved = chatRoomRepository.save(room);

                    addMembership(saved, userA);
                    addMembership(saved, userB);

                    logger.info("Created DM room '{}' with ID {} for users {} and {}",
                            dmName, saved.getId(), userA.getId(), userB.getId());
                    return saved;
                });
    }

    /**
     * Finds the DM room between two users without creating one.
     *
     * @param userA one participant
     * @param userB the other participant
     * @return Optional containing the DM room, or empty if none exists
     */
    public Optional<ChatRoom> findDmRoomBetween(User userA, User userB) {
        String dmName = buildDmName(userA, userB);
        return chatRoomRepository.findByNameAndRoomType(dmName, RoomType.DIRECT);
    }

    /**
     * Deletes a DM room and all its messages and memberships (via cascade).
     * Called when a friendship is dissolved.
     *
     * @param dmRoom the DM room to delete
     */
    @Transactional
    public void deleteDmRoom(ChatRoom dmRoom) {
        chatRoomRepository.delete(dmRoom);
        logger.info("Deleted DM room '{}' (id={})", dmRoom.getName(), dmRoom.getId());
    }

    /**
     * Builds the deterministic DM room name for a pair of users.
     * The lower user ID always comes first, so order of arguments does not matter.
     *
     * @param userA one participant
     * @param userB the other participant
     * @return deterministic room name string
     */
    private String buildDmName(User userA, User userB) {
        long minId = Math.min(userA.getId(), userB.getId());
        long maxId = Math.max(userA.getId(), userB.getId());
        return "dm__" + minId + "__" + maxId;
    }

    /**
     * Adds a user as a MEMBER of a DM room. Idempotent — does nothing if the
     * membership already exists.
     */
    private void addMembership(ChatRoom room, User user) {
        roomMembershipRepository.findByUserAndChatRoom(user, room)
                .orElseGet(() -> {
                    RoomMembership membership = new RoomMembership();
                    membership.setUser(user);
                    membership.setChatRoom(room);
                    membership.setJoinedAt(LocalDateTime.now());
                    membership.setRole(MemberRole.MEMBER);
                    return roomMembershipRepository.save(membership);
                });
    }
}
