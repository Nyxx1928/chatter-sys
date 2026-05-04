package org.example.chat.repository;

import org.example.chat.entity.Friendship;
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
 * Repository tests for Friendship entity.
 * Validates: Requirements 2.3, 8.1
 */
@DataJpaTest
class FriendshipRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FriendshipRepository friendshipRepository;

    private User userA;
    private User userB;
    private User userC;

    @BeforeEach
    void setUp() {
        // Create test users
        userA = new User();
        userA.setUsername("userA");
        userA.setEmail("userA@example.com");
        userA.setPasswordHash("password");
        userA.setDisplayName("User A");
        entityManager.persist(userA);

        userB = new User();
        userB.setUsername("userB");
        userB.setEmail("userB@example.com");
        userB.setPasswordHash("password");
        userB.setDisplayName("User B");
        entityManager.persist(userB);

        userC = new User();
        userC.setUsername("userC");
        userC.setEmail("userC@example.com");
        userC.setPasswordHash("password");
        userC.setDisplayName("User C");
        entityManager.persist(userC);

        entityManager.flush();
    }

    @Test
    void saveFriendship_ValidFriendship_PersistsSuccessfully() {
        // Arrange
        Friendship friendship = new Friendship();
        friendship.setUserA(userA);
        friendship.setUserB(userB);

        // Act
        Friendship saved = friendshipRepository.save(friendship);
        entityManager.flush();

        // Assert
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals(userA.getId(), saved.getUserA().getId());
        assertEquals(userB.getId(), saved.getUserB().getId());
    }

    @Test
    void findBetweenUsers_ExistingFriendship_ReturnsRegardlessOfOrder() {
        // Arrange
        Friendship friendship = new Friendship();
        friendship.setUserA(userA);
        friendship.setUserB(userB);
        entityManager.persist(friendship);
        entityManager.flush();

        // Act - Test both directions
        Optional<Friendship> foundAB = friendshipRepository.findBetweenUsers(userA, userB);
        Optional<Friendship> foundBA = friendshipRepository.findBetweenUsers(userB, userA);

        // Assert
        assertTrue(foundAB.isPresent());
        assertTrue(foundBA.isPresent());
        assertEquals(friendship.getId(), foundAB.get().getId());
        assertEquals(friendship.getId(), foundBA.get().getId());
    }

    @Test
    void findBetweenUsers_NoFriendship_ReturnsEmpty() {
        // Act
        Optional<Friendship> found = friendshipRepository.findBetweenUsers(userA, userB);

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void findByUserAOrUserB_MultipleFriendships_ReturnsAllForUser() {
        // Arrange
        Friendship friendship1 = new Friendship();
        friendship1.setUserA(userA);
        friendship1.setUserB(userB);
        entityManager.persist(friendship1);

        Friendship friendship2 = new Friendship();
        friendship2.setUserA(userC);
        friendship2.setUserB(userA);
        entityManager.persist(friendship2);

        Friendship friendship3 = new Friendship();
        friendship3.setUserA(userB);
        friendship3.setUserB(userC);
        entityManager.persist(friendship3);

        entityManager.flush();

        // Act
        List<Friendship> userAFriendships = friendshipRepository.findByUserAOrUserB(userA, userA);

        // Assert
        assertEquals(2, userAFriendships.size());
        assertTrue(userAFriendships.stream().anyMatch(f -> f.getId().equals(friendship1.getId())));
        assertTrue(userAFriendships.stream().anyMatch(f -> f.getId().equals(friendship2.getId())));
    }

    @Test
    void findByUserAOrUserB_NoFriendships_ReturnsEmptyList() {
        // Act
        List<Friendship> friendships = friendshipRepository.findByUserAOrUserB(userA, userA);

        // Assert
        assertTrue(friendships.isEmpty());
    }

    @Test
    void uniqueConstraint_DuplicateFriendship_ThrowsException() {
        // Arrange
        Friendship friendship1 = new Friendship();
        friendship1.setUserA(userA);
        friendship1.setUserB(userB);
        entityManager.persist(friendship1);
        entityManager.flush();

        Friendship friendship2 = new Friendship();
        friendship2.setUserA(userA);
        friendship2.setUserB(userB);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            entityManager.persist(friendship2);
            entityManager.flush();
        });
    }

    @Test
    void bidirectionalQuery_HandlesBothDirections() {
        // Arrange - Create friendship with userA as userA and userB as userB
        Friendship friendship = new Friendship();
        friendship.setUserA(userA);
        friendship.setUserB(userB);
        entityManager.persist(friendship);
        entityManager.flush();

        // Act - Query in both directions
        Optional<Friendship> foundForward = friendshipRepository.findBetweenUsers(userA, userB);
        Optional<Friendship> foundReverse = friendshipRepository.findBetweenUsers(userB, userA);

        // Assert - Both queries should find the same friendship
        assertTrue(foundForward.isPresent());
        assertTrue(foundReverse.isPresent());
        assertEquals(foundForward.get().getId(), foundReverse.get().getId());
    }
}
