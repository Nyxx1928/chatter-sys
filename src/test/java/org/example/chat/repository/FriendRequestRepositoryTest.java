package org.example.chat.repository;

import org.example.chat.entity.FriendRequest;
import org.example.chat.entity.FriendRequestStatus;
import org.example.chat.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository tests for FriendRequest entity.
 * Validates: Requirements 2.1, 2.3, 8.1
 */
@DataJpaTest
class FriendRequestRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    private User requester;
    private User recipient;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        // Create test users
        requester = new User();
        requester.setUsername("requester");
        requester.setEmail("requester@example.com");
        requester.setPasswordHash("password");
        requester.setDisplayName("Requester User");
        entityManager.persist(requester);

        recipient = new User();
        recipient.setUsername("recipient");
        recipient.setEmail("recipient@example.com");
        recipient.setPasswordHash("password");
        recipient.setDisplayName("Recipient User");
        entityManager.persist(recipient);

        anotherUser = new User();
        anotherUser.setUsername("another");
        anotherUser.setEmail("another@example.com");
        anotherUser.setPasswordHash("password");
        anotherUser.setDisplayName("Another User");
        entityManager.persist(anotherUser);

        entityManager.flush();
    }

    @Test
    void saveFriendRequest_ValidRequest_PersistsSuccessfully() {
        // Arrange
        FriendRequest request = new FriendRequest();
        request.setRequester(requester);
        request.setRecipient(recipient);
        request.setStatus(FriendRequestStatus.PENDING);

        // Act
        FriendRequest saved = friendRequestRepository.save(request);
        entityManager.flush();

        // Assert
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals(FriendRequestStatus.PENDING, saved.getStatus());
        assertEquals(requester.getId(), saved.getRequester().getId());
        assertEquals(recipient.getId(), saved.getRecipient().getId());
    }

    @Test
    void findByRequesterAndRecipient_ExistingRequest_ReturnsRequest() {
        // Arrange
        FriendRequest request = new FriendRequest();
        request.setRequester(requester);
        request.setRecipient(recipient);
        request.setStatus(FriendRequestStatus.PENDING);
        entityManager.persist(request);
        entityManager.flush();

        // Act
        Optional<FriendRequest> found = friendRequestRepository.findByRequesterAndRecipient(requester, recipient);

        // Assert
        assertTrue(found.isPresent());
        assertEquals(request.getId(), found.get().getId());
    }

    @Test
    void findByRequesterAndRecipient_NoRequest_ReturnsEmpty() {
        // Act
        Optional<FriendRequest> found = friendRequestRepository.findByRequesterAndRecipient(requester, recipient);

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void findByRecipientAndStatus_PendingRequests_ReturnsMatchingRequests() {
        // Arrange
        FriendRequest request1 = new FriendRequest();
        request1.setRequester(requester);
        request1.setRecipient(recipient);
        request1.setStatus(FriendRequestStatus.PENDING);
        entityManager.persist(request1);

        FriendRequest request2 = new FriendRequest();
        request2.setRequester(anotherUser);
        request2.setRecipient(recipient);
        request2.setStatus(FriendRequestStatus.ACCEPTED);
        entityManager.persist(request2);

        entityManager.flush();

        // Act
        List<FriendRequest> pendingRequests = friendRequestRepository.findByRecipientAndStatus(
            recipient, FriendRequestStatus.PENDING);

        // Assert
        assertEquals(1, pendingRequests.size());
        assertEquals(FriendRequestStatus.PENDING, pendingRequests.get(0).getStatus());
    }

    @Test
    void findByRequesterAndStatus_OutgoingRequests_ReturnsMatchingRequests() {
        // Arrange
        FriendRequest request = new FriendRequest();
        request.setRequester(requester);
        request.setRecipient(recipient);
        request.setStatus(FriendRequestStatus.PENDING);
        entityManager.persist(request);
        entityManager.flush();

        // Act
        List<FriendRequest> outgoingRequests = friendRequestRepository.findByRequesterAndStatus(
            requester, FriendRequestStatus.PENDING);

        // Assert
        assertEquals(1, outgoingRequests.size());
        assertEquals(requester.getId(), outgoingRequests.get(0).getRequester().getId());
    }

    @Test
    void findByIdAndRecipient_ValidRequest_ReturnsRequest() {
        // Arrange
        FriendRequest request = new FriendRequest();
        request.setRequester(requester);
        request.setRecipient(recipient);
        request.setStatus(FriendRequestStatus.PENDING);
        entityManager.persist(request);
        entityManager.flush();

        // Act
        Optional<FriendRequest> found = friendRequestRepository.findByIdAndRecipient(request.getId(), recipient);

        // Assert
        assertTrue(found.isPresent());
        assertEquals(request.getId(), found.get().getId());
    }

    @Test
    void findByIdAndRecipient_WrongRecipient_ReturnsEmpty() {
        // Arrange
        FriendRequest request = new FriendRequest();
        request.setRequester(requester);
        request.setRecipient(recipient);
        request.setStatus(FriendRequestStatus.PENDING);
        entityManager.persist(request);
        entityManager.flush();

        // Act
        Optional<FriendRequest> found = friendRequestRepository.findByIdAndRecipient(request.getId(), requester);

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void uniqueConstraint_DuplicateRequest_ThrowsException() {
        // Arrange
        FriendRequest request1 = new FriendRequest();
        request1.setRequester(requester);
        request1.setRecipient(recipient);
        request1.setStatus(FriendRequestStatus.PENDING);
        entityManager.persist(request1);
        entityManager.flush();

        FriendRequest request2 = new FriendRequest();
        request2.setRequester(requester);
        request2.setRecipient(recipient);
        request2.setStatus(FriendRequestStatus.PENDING);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            entityManager.persist(request2);
            entityManager.flush();
        });
    }
}
