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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FriendService.
 * **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 8.1**
 */
@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    private FriendService friendService;

    private User testUser;
    private User friendUser;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        friendService = new FriendService(userRepository, friendRequestRepository, friendshipRepository);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setDisplayName("Test User");
        testUser.setPasswordHash("hashedpassword");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setOnline(false);

        friendUser = new User();
        friendUser.setId(2L);
        friendUser.setUsername("frienduser");
        friendUser.setEmail("friend@example.com");
        friendUser.setDisplayName("Friend User");
        friendUser.setPasswordHash("hashedpassword");
        friendUser.setCreatedAt(LocalDateTime.now());
        friendUser.setOnline(false);

        anotherUser = new User();
        anotherUser.setId(3L);
        anotherUser.setUsername("anotheruser");
        anotherUser.setEmail("another@example.com");
        anotherUser.setDisplayName("Another User");
        anotherUser.setPasswordHash("hashedpassword");
        anotherUser.setCreatedAt(LocalDateTime.now());
        anotherUser.setOnline(true);
    }

    @Test
    void sendFriendRequest_ValidRequest_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(friendUser));
        when(friendshipRepository.findBetweenUsers(testUser, friendUser)).thenReturn(Optional.empty());
        when(friendRequestRepository.findByRequesterAndRecipient(testUser, friendUser)).thenReturn(Optional.empty());
        when(friendRequestRepository.findByRequesterAndRecipient(friendUser, testUser)).thenReturn(Optional.empty());
        when(friendRequestRepository.save(any(FriendRequest.class))).thenAnswer(invocation -> {
            FriendRequest request = invocation.getArgument(0);
            request.setId(1L);
            return request;
        });

        // Act
        FriendRequestResponse result = friendService.sendFriendRequest("testuser", 2L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getRequester().getUsername());
        assertEquals("frienduser", result.getRecipient().getUsername());
        assertNotNull(result.getCreatedAt());

        verify(userRepository).findByUsername("testuser");
        verify(userRepository).findById(2L);
        verify(friendshipRepository).findBetweenUsers(testUser, friendUser);
        verify(friendRequestRepository).findByRequesterAndRecipient(testUser, friendUser);
        verify(friendRequestRepository).findByRequesterAndRecipient(friendUser, testUser);
        verify(friendRequestRepository).save(any(FriendRequest.class));
    }

    @Test
    void sendFriendRequest_SelfRequest_ThrowsValidationException() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act & Assert
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> friendService.sendFriendRequest("testuser", 1L)
        );

        assertEquals("Cannot send friend request to yourself", exception.getMessage());
        verify(userRepository).findByUsername("testuser");
        verify(friendRequestRepository, never()).save(any(FriendRequest.class));
    }

    @Test
    void sendFriendRequest_NullRecipientId_ThrowsValidationException() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act & Assert
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> friendService.sendFriendRequest("testuser", null)
        );

        assertEquals("Recipient id is required", exception.getMessage());
        verify(userRepository).findByUsername("testuser");
        verify(friendRequestRepository, never()).save(any(FriendRequest.class));
    }

    @Test
    void sendFriendRequest_RecipientNotFound_ThrowsUserNotFoundException() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        UserNotFoundException exception = assertThrows(
            UserNotFoundException.class,
            () -> friendService.sendFriendRequest("testuser", 999L)
        );

        assertTrue(exception.getMessage().contains("999"));
        verify(userRepository).findByUsername("testuser");
        verify(userRepository).findById(999L);
        verify(friendRequestRepository, never()).save(any(FriendRequest.class));
    }

    @Test
    void sendFriendRequest_AlreadyFriends_ThrowsConflictException() {
        // Arrange
        Friendship existingFriendship = new Friendship();
        existingFriendship.setId(1L);
        existingFriendship.setUserA(testUser);
        existingFriendship.setUserB(friendUser);
        existingFriendship.setCreatedAt(LocalDateTime.now());

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(friendUser));
        when(friendshipRepository.findBetweenUsers(testUser, friendUser)).thenReturn(Optional.of(existingFriendship));

        // Act & Assert
        ConflictException exception = assertThrows(
            ConflictException.class,
            () -> friendService.sendFriendRequest("testuser", 2L)
        );

        assertEquals("You are already friends with this user", exception.getMessage());
        verify(friendRequestRepository, never()).save(any(FriendRequest.class));
    }

    @Test
    void sendFriendRequest_DuplicateRequest_ThrowsConflictException() {
        // Arrange
        FriendRequest existingRequest = new FriendRequest();
        existingRequest.setId(1L);
        existingRequest.setRequester(testUser);
        existingRequest.setRecipient(friendUser);
        existingRequest.setStatus(FriendRequestStatus.PENDING);
        existingRequest.setCreatedAt(LocalDateTime.now());

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(friendUser));
        when(friendshipRepository.findBetweenUsers(testUser, friendUser)).thenReturn(Optional.empty());
        when(friendRequestRepository.findByRequesterAndRecipient(testUser, friendUser))
            .thenReturn(Optional.of(existingRequest));

        // Act & Assert
        ConflictException exception = assertThrows(
            ConflictException.class,
            () -> friendService.sendFriendRequest("testuser", 2L)
        );

        assertEquals("Friend request already sent", exception.getMessage());
        verify(friendRequestRepository, never()).save(any(FriendRequest.class));
    }

    @Test
    void sendFriendRequest_IncomingRequestExists_ThrowsConflictException() {
        // Arrange
        FriendRequest incomingRequest = new FriendRequest();
        incomingRequest.setId(1L);
        incomingRequest.setRequester(friendUser);
        incomingRequest.setRecipient(testUser);
        incomingRequest.setStatus(FriendRequestStatus.PENDING);
        incomingRequest.setCreatedAt(LocalDateTime.now());

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(friendUser));
        when(friendshipRepository.findBetweenUsers(testUser, friendUser)).thenReturn(Optional.empty());
        when(friendRequestRepository.findByRequesterAndRecipient(testUser, friendUser)).thenReturn(Optional.empty());
        when(friendRequestRepository.findByRequesterAndRecipient(friendUser, testUser))
            .thenReturn(Optional.of(incomingRequest));

        // Act & Assert
        ConflictException exception = assertThrows(
            ConflictException.class,
            () -> friendService.sendFriendRequest("testuser", 2L)
        );

        assertEquals("Friend request already received from this user", exception.getMessage());
        verify(friendRequestRepository, never()).save(any(FriendRequest.class));
    }

    @Test
    void listPendingRequests_ReturnsIncomingAndOutgoing() {
        // Arrange
        FriendRequest incomingRequest = new FriendRequest();
        incomingRequest.setId(1L);
        incomingRequest.setRequester(friendUser);
        incomingRequest.setRecipient(testUser);
        incomingRequest.setStatus(FriendRequestStatus.PENDING);
        incomingRequest.setCreatedAt(LocalDateTime.now());

        FriendRequest outgoingRequest = new FriendRequest();
        outgoingRequest.setId(2L);
        outgoingRequest.setRequester(testUser);
        outgoingRequest.setRecipient(anotherUser);
        outgoingRequest.setStatus(FriendRequestStatus.PENDING);
        outgoingRequest.setCreatedAt(LocalDateTime.now());

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(friendRequestRepository.findByRecipientAndStatus(testUser, FriendRequestStatus.PENDING))
            .thenReturn(List.of(incomingRequest));
        when(friendRequestRepository.findByRequesterAndStatus(testUser, FriendRequestStatus.PENDING))
            .thenReturn(List.of(outgoingRequest));

        // Act
        FriendRequestListResponse result = friendService.listPendingRequests("testuser");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getIncoming().size());
        assertEquals(1, result.getOutgoing().size());
        assertEquals("frienduser", result.getIncoming().get(0).getRequester().getUsername());
        assertEquals("anotheruser", result.getOutgoing().get(0).getRecipient().getUsername());

        verify(userRepository).findByUsername("testuser");
        verify(friendRequestRepository).findByRecipientAndStatus(testUser, FriendRequestStatus.PENDING);
        verify(friendRequestRepository).findByRequesterAndStatus(testUser, FriendRequestStatus.PENDING);
    }

    @Test
    void listPendingRequests_EmptyLists_ReturnsEmptyLists() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(friendRequestRepository.findByRecipientAndStatus(testUser, FriendRequestStatus.PENDING))
            .thenReturn(List.of());
        when(friendRequestRepository.findByRequesterAndStatus(testUser, FriendRequestStatus.PENDING))
            .thenReturn(List.of());

        // Act
        FriendRequestListResponse result = friendService.listPendingRequests("testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.getIncoming().isEmpty());
        assertTrue(result.getOutgoing().isEmpty());
    }

    @Test
    void acceptFriendRequest_ValidRequest_CreatesFriendship() {
        // Arrange
        FriendRequest request = new FriendRequest();
        request.setId(1L);
        request.setRequester(friendUser);
        request.setRecipient(testUser);
        request.setStatus(FriendRequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());

        Friendship friendship = new Friendship();
        friendship.setId(1L);
        friendship.setUserA(testUser);
        friendship.setUserB(friendUser);
        friendship.setCreatedAt(LocalDateTime.now());

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(friendRequestRepository.findByIdAndRecipient(1L, testUser)).thenReturn(Optional.of(request));
        when(friendshipRepository.findBetweenUsers(testUser, friendUser)).thenReturn(Optional.empty());
        when(friendshipRepository.save(any(Friendship.class))).thenReturn(friendship);

        // Act
        FriendshipResponse result = friendService.acceptFriendRequest("testuser", 1L);

        // Assert
        assertNotNull(result);
        assertEquals("frienduser", result.getFriend().getUsername());
        assertNotNull(result.getCreatedAt());

        verify(userRepository).findByUsername("testuser");
        verify(friendRequestRepository).findByIdAndRecipient(1L, testUser);
        verify(friendshipRepository).findBetweenUsers(testUser, friendUser);
        verify(friendshipRepository).save(any(Friendship.class));
        verify(friendRequestRepository).delete(request);
    }

    @Test
    void acceptFriendRequest_RequestNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(friendRequestRepository.findByIdAndRecipient(999L, testUser)).thenReturn(Optional.empty());

        // Act & Assert
        FriendRequestNotFoundException exception = assertThrows(
            FriendRequestNotFoundException.class,
            () -> friendService.acceptFriendRequest("testuser", 999L)
        );

        assertTrue(exception.getMessage().contains("999"));
        verify(friendshipRepository, never()).save(any(Friendship.class));
        verify(friendRequestRepository, never()).delete(any(FriendRequest.class));
    }

    @Test
    void acceptFriendRequest_FriendshipAlreadyExists_ReturnsExistingFriendship() {
        // Arrange
        FriendRequest request = new FriendRequest();
        request.setId(1L);
        request.setRequester(friendUser);
        request.setRecipient(testUser);
        request.setStatus(FriendRequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());

        Friendship existingFriendship = new Friendship();
        existingFriendship.setId(1L);
        existingFriendship.setUserA(testUser);
        existingFriendship.setUserB(friendUser);
        existingFriendship.setCreatedAt(LocalDateTime.now());

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(friendRequestRepository.findByIdAndRecipient(1L, testUser)).thenReturn(Optional.of(request));
        when(friendshipRepository.findBetweenUsers(testUser, friendUser)).thenReturn(Optional.of(existingFriendship));

        // Act
        FriendshipResponse result = friendService.acceptFriendRequest("testuser", 1L);

        // Assert
        assertNotNull(result);
        assertEquals("frienduser", result.getFriend().getUsername());

        verify(friendshipRepository, never()).save(any(Friendship.class));
        verify(friendRequestRepository).delete(request);
    }

    @Test
    void declineFriendRequest_ValidRequest_DeletesRequest() {
        // Arrange
        FriendRequest request = new FriendRequest();
        request.setId(1L);
        request.setRequester(friendUser);
        request.setRecipient(testUser);
        request.setStatus(FriendRequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(friendRequestRepository.findByIdAndRecipient(1L, testUser)).thenReturn(Optional.of(request));

        // Act
        friendService.declineFriendRequest("testuser", 1L);

        // Assert
        verify(userRepository).findByUsername("testuser");
        verify(friendRequestRepository).findByIdAndRecipient(1L, testUser);
        verify(friendRequestRepository).delete(request);
    }

    @Test
    void declineFriendRequest_RequestNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(friendRequestRepository.findByIdAndRecipient(999L, testUser)).thenReturn(Optional.empty());

        // Act & Assert
        FriendRequestNotFoundException exception = assertThrows(
            FriendRequestNotFoundException.class,
            () -> friendService.declineFriendRequest("testuser", 999L)
        );

        assertTrue(exception.getMessage().contains("999"));
        verify(friendRequestRepository, never()).delete(any(FriendRequest.class));
    }

    @Test
    void listFriends_ReturnsFriendsList() {
        // Arrange
        Friendship friendship1 = new Friendship();
        friendship1.setId(1L);
        friendship1.setUserA(testUser);
        friendship1.setUserB(friendUser);
        friendship1.setCreatedAt(LocalDateTime.now());

        Friendship friendship2 = new Friendship();
        friendship2.setId(2L);
        friendship2.setUserA(anotherUser);
        friendship2.setUserB(testUser);
        friendship2.setCreatedAt(LocalDateTime.now());

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(friendshipRepository.findByUserAOrUserB(testUser, testUser))
            .thenReturn(List.of(friendship1, friendship2));

        // Act
        List<PublicUserResponse> result = friendService.listFriends("testuser");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(u -> u.getUsername().equals("frienduser")));
        assertTrue(result.stream().anyMatch(u -> u.getUsername().equals("anotheruser")));

        verify(userRepository).findByUsername("testuser");
        verify(friendshipRepository).findByUserAOrUserB(testUser, testUser);
    }

    @Test
    void listFriends_EmptyList_ReturnsEmptyList() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(friendshipRepository.findByUserAOrUserB(testUser, testUser)).thenReturn(List.of());

        // Act
        List<PublicUserResponse> result = friendService.listFriends("testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void searchUsers_EmptyQuery_ReturnsEmptyList() {
        // Act
        List<UserSearchResultResponse> result = friendService.searchUsers("", "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, never()).findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(anyString(), anyString());
    }

    @Test
    void searchUsers_NullQuery_ReturnsEmptyList() {
        // Act
        List<UserSearchResultResponse> result = friendService.searchUsers(null, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, never()).findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(anyString(), anyString());
    }

    @Test
    void searchUsers_ValidQuery_ReturnsUsersWithRelationshipStatus() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase("friend", "friend"))
            .thenReturn(List.of(friendUser, anotherUser));
        when(friendshipRepository.findByUserAOrUserB(testUser, testUser)).thenReturn(List.of());
        when(friendRequestRepository.findByRecipientAndStatus(testUser, FriendRequestStatus.PENDING))
            .thenReturn(List.of());
        when(friendRequestRepository.findByRequesterAndStatus(testUser, FriendRequestStatus.PENDING))
            .thenReturn(List.of());

        // Act
        List<UserSearchResultResponse> result = friendService.searchUsers("friend", "testuser");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(r -> r.getRelationshipStatus() == RelationshipStatus.NONE));
    }

    @Test
    void searchUsers_ExcludesCurrentUser() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase("test", "test"))
            .thenReturn(List.of(testUser, friendUser));
        when(friendshipRepository.findByUserAOrUserB(testUser, testUser)).thenReturn(List.of());
        when(friendRequestRepository.findByRecipientAndStatus(testUser, FriendRequestStatus.PENDING))
            .thenReturn(List.of());
        when(friendRequestRepository.findByRequesterAndStatus(testUser, FriendRequestStatus.PENDING))
            .thenReturn(List.of());

        // Act
        List<UserSearchResultResponse> result = friendService.searchUsers("test", "testuser");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("frienduser", result.get(0).getUser().getUsername());
    }
}
