package org.example.chat.service;

import org.example.chat.dto.*;
import org.example.chat.entity.FriendRequest;
import org.example.chat.entity.FriendRequestStatus;
import org.example.chat.entity.Friendship;
import org.example.chat.entity.User;
import org.example.chat.exception.ConflictException;
import org.example.chat.exception.FriendRequestNotFoundException;
import org.example.chat.exception.UserNotFoundException;
import org.example.chat.exception.ValidationException;
import org.example.chat.repository.FriendRequestRepository;
import org.example.chat.repository.FriendshipRepository;
import org.example.chat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for user search, friend requests, and friendships.
 */
@Service
public class FriendService {

    private static final Logger logger = LoggerFactory.getLogger(FriendService.class);

    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final DirectMessageService directMessageService;

    public FriendService(UserRepository userRepository,
            FriendRequestRepository friendRequestRepository,
            FriendshipRepository friendshipRepository,
            DirectMessageService directMessageService) {
        this.userRepository = userRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.friendshipRepository = friendshipRepository;
        this.directMessageService = directMessageService;
    }

    public List<UserSearchResultResponse> searchUsers(String query, String currentUsername) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        User currentUser = getUserByUsername(currentUsername);
        String trimmedQuery = query.trim();

        List<User> matches = userRepository
                .findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(trimmedQuery, trimmedQuery)
                .stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .collect(Collectors.toList());

        if (matches.isEmpty()) {
            return List.of();
        }

        Set<Long> friendIds = getFriendIds(currentUser);
        Map<Long, RelationshipStatus> pendingIncoming = getPendingIncoming(currentUser);
        Map<Long, RelationshipStatus> pendingOutgoing = getPendingOutgoing(currentUser);

        return matches.stream()
                .map(user -> new UserSearchResultResponse(
                        PublicUserResponse.from(user),
                        resolveRelationshipStatus(user.getId(), friendIds, pendingIncoming, pendingOutgoing)))
                .collect(Collectors.toList());
    }

    @Transactional
    public FriendRequestResponse sendFriendRequest(String currentUsername, Long recipientId) {
        User requester = getUserByUsername(currentUsername);

        if (recipientId == null) {
            throw new ValidationException("Recipient id is required");
        }

        if (requester.getId().equals(recipientId)) {
            throw new ValidationException("Cannot send friend request to yourself");
        }

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new UserNotFoundException(recipientId));

        if (friendshipRepository.findBetweenUsers(requester, recipient).isPresent()) {
            throw new ConflictException("You are already friends with this user");
        }

        if (friendRequestRepository.findByRequesterAndRecipient(requester, recipient).isPresent()) {
            throw new ConflictException("Friend request already sent");
        }

        if (friendRequestRepository.findByRequesterAndRecipient(recipient, requester).isPresent()) {
            throw new ConflictException("Friend request already received from this user");
        }

        FriendRequest request = new FriendRequest();
        request.setRequester(requester);
        request.setRecipient(recipient);
        request.setStatus(FriendRequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());

        FriendRequest savedRequest = friendRequestRepository.save(request);
        logger.info("Friend request sent from {} to {}", requester.getUsername(), recipient.getUsername());

        return FriendRequestResponse.from(savedRequest);
    }

    public FriendRequestListResponse listPendingRequests(String currentUsername) {
        User currentUser = getUserByUsername(currentUsername);

        List<FriendRequestResponse> incoming = friendRequestRepository
                .findByRecipientAndStatus(currentUser, FriendRequestStatus.PENDING)
                .stream()
                .map(FriendRequestResponse::from)
                .collect(Collectors.toList());

        List<FriendRequestResponse> outgoing = friendRequestRepository
                .findByRequesterAndStatus(currentUser, FriendRequestStatus.PENDING)
                .stream()
                .map(FriendRequestResponse::from)
                .collect(Collectors.toList());

        return new FriendRequestListResponse(incoming, outgoing);
    }

    @Transactional
    public FriendshipResponse acceptFriendRequest(String currentUsername, Long requestId) {
        User currentUser = getUserByUsername(currentUsername);
        FriendRequest request = friendRequestRepository.findByIdAndRecipient(requestId, currentUser)
                .orElseThrow(() -> new FriendRequestNotFoundException(requestId));

        User requester = request.getRequester();

        Friendship friendship = friendshipRepository.findBetweenUsers(currentUser, requester)
                .orElseGet(() -> createFriendship(currentUser, requester));

        friendRequestRepository.delete(request);

        logger.info("Friend request accepted by {} from {}", currentUser.getUsername(), requester.getUsername());
        User friend = friendship.getUserA().getId().equals(currentUser.getId())
                ? friendship.getUserB()
                : friendship.getUserA();

        // Auto-create the DM room for this friendship
        var dmRoom = directMessageService.getOrCreateDmRoom(currentUser, requester);

        return new FriendshipResponse(PublicUserResponse.from(friend), friendship.getCreatedAt(), dmRoom.getId());
    }

    @Transactional
    public void declineFriendRequest(String currentUsername, Long requestId) {
        User currentUser = getUserByUsername(currentUsername);
        FriendRequest request = friendRequestRepository.findByIdAndRecipient(requestId, currentUser)
                .orElseThrow(() -> new FriendRequestNotFoundException(requestId));

        friendRequestRepository.delete(request);

        logger.info("Friend request declined by {} from {}", currentUser.getUsername(), request.getRequester().getUsername());
    }

    public List<PublicUserResponse> listFriends(String currentUsername) {
        User currentUser = getUserByUsername(currentUsername);

        return friendshipRepository.findByUserAOrUserB(currentUser, currentUser).stream()
                .map(friendship -> {
                    User friend = friendship.getUserA().getId().equals(currentUser.getId())
                            ? friendship.getUserB()
                            : friendship.getUserA();
                    return PublicUserResponse.from(friend);
                })
                .collect(Collectors.toList());
    }

    /**
     * Removes a friendship between the current user and the specified friend.
     * Also deletes the shared DM room (and all its messages) if one exists.
     *
     * @param currentUsername the username of the user initiating the removal
     * @param friendId        the ID of the friend to remove
     * @throws UserNotFoundException if the friend user is not found
     * @throws ConflictException     if the two users are not friends
     */
    @Transactional
    public void removeFriend(String currentUsername, Long friendId) {
        User currentUser = getUserByUsername(currentUsername);
        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new UserNotFoundException(friendId));

        Friendship friendship = friendshipRepository.findBetweenUsers(currentUser, friend)
                .orElseThrow(() -> new ConflictException("You are not friends with this user"));

        // Delete the DM room between the two users (if it exists)
        directMessageService.findDmRoomBetween(currentUser, friend)
                .ifPresent(dmRoom -> {
                    logger.info("Deleting DM room '{}' as part of unfriend between {} and {}",
                            dmRoom.getName(), currentUser.getUsername(), friend.getUsername());
                    // DirectMessageService uses ChatRoomRepository — delete via it
                    // so cascades (messages, memberships) fire correctly
                    directMessageService.deleteDmRoom(dmRoom);
                });

        friendshipRepository.delete(friendship);
        logger.info("Removed friendship between {} and {}", currentUser.getUsername(), friend.getUsername());
    }

    private Friendship createFriendship(User userA, User userB) {
        User first = userA.getId() < userB.getId() ? userA : userB;
        User second = userA.getId() < userB.getId() ? userB : userA;

        Friendship friendship = new Friendship();
        friendship.setUserA(first);
        friendship.setUserB(second);
        friendship.setCreatedAt(LocalDateTime.now());

        return friendshipRepository.save(friendship);
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
    }

    private Set<Long> getFriendIds(User currentUser) {
        return friendshipRepository.findByUserAOrUserB(currentUser, currentUser).stream()
                .map(friendship -> friendship.getUserA().getId().equals(currentUser.getId())
                        ? friendship.getUserB().getId()
                        : friendship.getUserA().getId())
                .collect(Collectors.toSet());
    }

    private Map<Long, RelationshipStatus> getPendingIncoming(User currentUser) {
        Map<Long, RelationshipStatus> incoming = new HashMap<>();

        friendRequestRepository.findByRecipientAndStatus(currentUser, FriendRequestStatus.PENDING)
                .forEach(request -> incoming.put(request.getRequester().getId(), RelationshipStatus.PENDING_INCOMING));

        return incoming;
    }

    private Map<Long, RelationshipStatus> getPendingOutgoing(User currentUser) {
        Map<Long, RelationshipStatus> outgoing = new HashMap<>();

        friendRequestRepository.findByRequesterAndStatus(currentUser, FriendRequestStatus.PENDING)
                .forEach(request -> outgoing.put(request.getRecipient().getId(), RelationshipStatus.PENDING_OUTGOING));

        return outgoing;
    }

    private RelationshipStatus resolveRelationshipStatus(
            Long userId,
            Set<Long> friendIds,
            Map<Long, RelationshipStatus> pendingIncoming,
            Map<Long, RelationshipStatus> pendingOutgoing) {
        if (friendIds.contains(userId)) {
            return RelationshipStatus.FRIENDS;
        }

        if (pendingIncoming.containsKey(userId)) {
            return RelationshipStatus.PENDING_INCOMING;
        }

        if (pendingOutgoing.containsKey(userId)) {
            return RelationshipStatus.PENDING_OUTGOING;
        }

        return RelationshipStatus.NONE;
    }
}
