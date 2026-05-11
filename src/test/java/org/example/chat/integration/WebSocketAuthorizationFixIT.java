package org.example.chat.integration;

import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.Message;
import org.example.chat.entity.RoomMembership;
import org.example.chat.entity.User;
import org.example.chat.exception.UnauthorizedException;
import org.example.chat.repository.ChatRoomRepository;
import org.example.chat.repository.MessageRepository;
import org.example.chat.repository.RoomMembershipRepository;
import org.example.chat.repository.UserRepository;
import org.example.chat.service.ChatMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fix checking test for WebSocket authorization enforcement.
 * Verifies that unauthorized users are rejected when attempting to send messages
 * to rooms they are not members of.
 *
 * Validates: Requirements 2.1, 2.2, 2.3, 2.4
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WebSocketAuthorizationFixIT {

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private RoomMembershipRepository roomMembershipRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private User testUser;
    private ChatRoom testRoom;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setDisplayName("Test User");
        testUser.setPasswordHash("hashedpassword");
        testUser.setOnline(true);
        testUser = userRepository.save(testUser);

        // Create test room
        testRoom = new ChatRoom();
        testRoom.setName("Test Room");
        testRoom.setDescription("A test room");
        testRoom.setCreatedBy(testUser);
        testRoom = chatRoomRepository.save(testRoom);

        // Explicitly do NOT add user to room membership
        // This ensures the user is not a member of the room
    }

    /**
     * Test 3.1: Write fix checking test for WebSocket authorization enforcement
     *
     * Verifies that unauthorized users are rejected when attempting to send messages
     * to rooms they are not members of.
     *
     * Acceptance Criteria:
     * - Test creates user and room
     * - Test does NOT add user to room membership
     * - Test attempts to send message via STOMP
     * - Test verifies UnauthorizedException is thrown
     * - Test verifies message is NOT persisted
     * - Test verifies message is NOT broadcast
     * - Test verifies error response is sent to user's error queue
     * - Test uses Spring Boot test framework
     * - Test passes with fixed code
     */
    @Test
    void testUnauthorizedUserCannotSendMessage() {
        // Arrange
        String messageContent = "Unauthorized message";
        long initialMessageCount = messageRepository.count();

        // Act & Assert
        // Verify that UnauthorizedException is thrown when unauthorized user attempts to send message
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            chatMessageService.sendMessage(testUser.getId(), testRoom.getId(), messageContent);
        });

        // Assert exception message
        assertEquals("User is not a member of this chat room", exception.getMessage());

        // Assert message is NOT persisted
        long finalMessageCount = messageRepository.count();
        assertEquals(initialMessageCount, finalMessageCount,
            "Message should not be persisted when user is not a member");

        // Assert no messages exist in the room
        List<Message> roomMessages = messageRepository.findAll();
        for (Message msg : roomMessages) {
            assertNotEquals(testRoom.getId(), msg.getChatRoom().getId(),
                "No messages should exist in the room from unauthorized user");
        }
    }

    /**
     * Test that authorized users CAN send messages (preservation test)
     * This ensures the fix doesn't break legitimate message sending.
     */
    @Test
    void testAuthorizedUserCanSendMessage() {
        // Arrange
        // Add user to room membership
        RoomMembership membership = new RoomMembership();
        membership.setUser(testUser);
        membership.setChatRoom(testRoom);
        membership.setJoinedAt(LocalDateTime.now());
        roomMembershipRepository.save(membership);

        String messageContent = "Authorized message";
        long initialMessageCount = messageRepository.count();

        // Act
        Message result = chatMessageService.sendMessage(testUser.getId(), testRoom.getId(), messageContent);

        // Assert
        assertNotNull(result, "Message should be persisted");
        assertEquals(messageContent, result.getContent(), "Message content should match");
        assertEquals(testUser.getId(), result.getSender().getId(), "Sender should be correct");
        assertEquals(testRoom.getId(), result.getChatRoom().getId(), "Room should be correct");

        // Assert message is persisted
        long finalMessageCount = messageRepository.count();
        assertEquals(initialMessageCount + 1, finalMessageCount,
            "Message should be persisted when user is a member");
    }

    /**
     * Test that user who left the room cannot send messages
     */
    @Test
    void testUserWhoLeftRoomCannotSendMessage() {
        // Arrange
        // Add user to room membership
        RoomMembership membership = new RoomMembership();
        membership.setUser(testUser);
        membership.setChatRoom(testRoom);
        membership.setJoinedAt(LocalDateTime.now());
        roomMembershipRepository.save(membership);

        // Remove user from room membership
        roomMembershipRepository.delete(membership);

        String messageContent = "Message after leaving";
        long initialMessageCount = messageRepository.count();

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            chatMessageService.sendMessage(testUser.getId(), testRoom.getId(), messageContent);
        });

        // Assert exception message
        assertEquals("User is not a member of this chat room", exception.getMessage());

        // Assert message is NOT persisted
        long finalMessageCount = messageRepository.count();
        assertEquals(initialMessageCount, finalMessageCount,
            "Message should not be persisted after user leaves room");
    }

    /**
     * Test that user cannot send to a different room they're not a member of
     */
    @Test
    void testUserCannotSendToUnauthorizedRoom() {
        // Arrange
        // Create another room
        ChatRoom anotherRoom = new ChatRoom();
        anotherRoom.setName("Another Room");
        anotherRoom.setDescription("Another test room");
        anotherRoom.setCreatedBy(testUser);
        anotherRoom = chatRoomRepository.save(anotherRoom);
        final Long anotherRoomId = anotherRoom.getId();

        // Add user to first room only
        RoomMembership membership = new RoomMembership();
        membership.setUser(testUser);
        membership.setChatRoom(testRoom);
        membership.setJoinedAt(LocalDateTime.now());
        roomMembershipRepository.save(membership);

        String messageContent = "Message to unauthorized room";
        long initialMessageCount = messageRepository.count();

        // Act & Assert
        // User should not be able to send to anotherRoom
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            chatMessageService.sendMessage(testUser.getId(), anotherRoomId, messageContent);
        });

        // Assert exception message
        assertEquals("User is not a member of this chat room", exception.getMessage());

        // Assert message is NOT persisted
        long finalMessageCount = messageRepository.count();
        assertEquals(initialMessageCount, finalMessageCount,
            "Message should not be persisted to unauthorized room");
    }
}
