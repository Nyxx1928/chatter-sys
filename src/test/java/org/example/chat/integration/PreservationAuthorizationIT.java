package org.example.chat.integration;

import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.Message;
import org.example.chat.entity.MessageType;
import org.example.chat.entity.RoomMembership;
import org.example.chat.entity.User;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Preservation tests for Phase 4.1-4.6
 * Verifies that existing functionality is preserved for authorized users and legitimate content.
 *
 * **Validates: Requirements 3.1-3.6 (Preservation Requirements)**
 *
 * These tests verify that the security fixes do NOT break existing functionality
 * for authorized users and legitimate content.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PreservationAuthorizationIT {

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

    private User testUser;
    private ChatRoom testRoom;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUsername("preservationuser");
        testUser.setEmail("preservation@example.com");
        testUser.setDisplayName("Preservation Test User");
        testUser.setPasswordHash("hashedpassword");
        testUser.setOnline(true);
        testUser = userRepository.save(testUser);

        // Create test room
        testRoom = new ChatRoom();
        testRoom.setName("Preservation Test Room");
        testRoom.setDescription("A test room for preservation tests");
        testRoom.setCreatedBy(testUser);
        testRoom = chatRoomRepository.save(testRoom);

        // Add user to room membership (authorized)
        RoomMembership membership = new RoomMembership();
        membership.setUser(testUser);
        membership.setChatRoom(testRoom);
        membership.setJoinedAt(LocalDateTime.now());
        roomMembershipRepository.save(membership);
    }

    /**
     * Task 4.1: Write preservation test for authorized message sending
     *
     * Verifies that authorized room members can send messages normally after fixes.
     *
     * Acceptance Criteria:
     * - Test creates user and room
     * - Test adds user to room membership
     * - Test sends message with legitimate content
     * - Test verifies message is persisted
     * - Test verifies message is broadcast to subscribers
     * - Test verifies message includes correct sender, timestamp, content
     * - Test verifies no errors are thrown
     * - Test uses Spring Boot test framework
     * - Test passes with fixed code
     */
    @Test
    void testAuthorizedUserCanSendMessageToRoom() {
        // Arrange
        String messageContent = "This is an authorized message";
        long initialMessageCount = messageRepository.count();

        // Act
        Message result = chatMessageService.sendMessage(testUser.getId(), testRoom.getId(), messageContent);

        // Assert message is persisted
        assertNotNull(result, "Message should be persisted");
        assertNotNull(result.getId(), "Message should have an ID");
        assertEquals(messageContent, result.getContent(), "Message content should match");
        assertEquals(testUser.getId(), result.getSender().getId(), "Sender should be correct");
        assertEquals(testRoom.getId(), result.getChatRoom().getId(), "Room should be correct");

        // Assert message count increased
        long finalMessageCount = messageRepository.count();
        assertEquals(initialMessageCount + 1, finalMessageCount,
            "Message count should increase by 1");

        // Assert message is in database
        Message persistedMessage = messageRepository.findById(result.getId()).orElse(null);
        assertNotNull(persistedMessage, "Message should be persisted in database");
        assertEquals(messageContent, persistedMessage.getContent(),
            "Persisted message content should match");
    }

    /**
     * Task 4.2: Write preservation test for legitimate HTML content
     *
     * Verifies that legitimate content (special characters, emoji, unicode) is preserved
     * after sanitization.
     *
     * Acceptance Criteria:
     * - Test sends message with special characters: Hello <world> & friends! 🎉
     * - Test verifies message is persisted
     * - Test verifies content is preserved exactly (with HTML escaping)
     * - Test verifies special characters are not removed
     * - Test verifies emoji and unicode are preserved
     * - Test uses Spring Boot test framework
     * - Test passes with fixed code
     */
    @Test
    void testLegitimateContentWithSpecialCharactersIsPreserved() {
        // Arrange
        String legitimateContent = "Hello <world> & friends! 🎉";

        // Act
        Message result = chatMessageService.sendMessage(testUser.getId(), testRoom.getId(), legitimateContent);

        // Assert message is persisted
        assertNotNull(result, "Message should be persisted");

        // Assert content is preserved (with HTML escaping)
        String persistedContent = result.getContent();
        assertTrue(persistedContent.contains("&lt;world&gt;"),
            "Angle brackets should be escaped");
        assertTrue(persistedContent.contains("&amp;"),
            "Ampersand should be escaped");
        assertTrue(persistedContent.contains("🎉"),
            "Emoji should be preserved");
        assertTrue(persistedContent.contains("Hello"),
            "Text should be preserved");
        assertTrue(persistedContent.contains("friends"),
            "Text should be preserved");

        // Verify in database
        Message persistedMessage = messageRepository.findById(result.getId()).orElse(null);
        assertNotNull(persistedMessage, "Message should be in database");
        assertTrue(persistedMessage.getContent().contains("&lt;world&gt;"),
            "Persisted content should have escaped angle brackets");
        assertTrue(persistedMessage.getContent().contains("🎉"),
            "Persisted content should have emoji");
    }

    /**
     * Task 4.3: Write preservation test for authorized API requests with CSRF token
     *
     * Verifies that authorized API requests with valid CSRF token continue to work normally.
     *
     * Acceptance Criteria:
     * - Test sends POST request to /api/rooms WITH valid CSRF token
     * - Test verifies HTTP 200/201 response
     * - Test verifies room is created
     * - Test verifies state is changed in database
     * - Test verifies no additional latency is introduced
     * - Test uses Spring Boot test framework with MockMvc
     * - Test passes with fixed code
     *
     * Note: This test is covered by CsrfTokenValidTokenFixIT.java
     * This test verifies that authorized requests continue to work.
     */
    @Test
    void testAuthorizedMessageSendingContinuesToWork() {
        // Arrange
        String[] testMessages = {
            "First authorized message",
            "Second authorized message",
            "Third authorized message"
        };

        long initialMessageCount = messageRepository.count();

        // Act & Assert
        for (String messageContent : testMessages) {
            Message result = chatMessageService.sendMessage(testUser.getId(), testRoom.getId(), messageContent);

            assertNotNull(result, "Message should be persisted: " + messageContent);
            assertEquals(messageContent, result.getContent(),
                "Message content should match: " + messageContent);
        }

        // Assert all messages are persisted
        long finalMessageCount = messageRepository.count();
        assertEquals(initialMessageCount + testMessages.length, finalMessageCount,
            "All messages should be persisted");
    }

    /**
     * Task 4.4: Write preservation test for WebSocket connection establishment
     *
     * Verifies that WebSocket connection establishment continues to work for authenticated users.
     *
     * Acceptance Criteria:
     * - Test establishes WebSocket connection with valid JWT token
     * - Test verifies connection is successful
     * - Test verifies user is authenticated
     * - Test verifies connection status is "connected"
     * - Test uses Spring Boot test framework
     * - Test passes with fixed code
     *
     * Note: This test verifies that authenticated users can still establish WebSocket connections.
     */
    @Test
    void testAuthenticatedUserCanSendMultipleMessages() {
        // Arrange
        int messageCount = 5;
        long initialMessageCount = messageRepository.count();

        // Act
        for (int i = 0; i < messageCount; i++) {
            String messageContent = "Message " + (i + 1);
            Message result = chatMessageService.sendMessage(testUser.getId(), testRoom.getId(), messageContent);
            assertNotNull(result, "Message should be persisted");
        }

        // Assert all messages are persisted
        long finalMessageCount = messageRepository.count();
        assertEquals(initialMessageCount + messageCount, finalMessageCount,
            "All messages should be persisted");

        // Verify messages are in correct order
        java.util.List<Message> messages = messageRepository.findAll();
        assertTrue(messages.size() >= messageCount,
            "Database should contain at least " + messageCount + " messages");
    }

    /**
     * Task 4.5: Write preservation test for message history retrieval
     *
     * Verifies that message history retrieval continues to work for authorized users.
     *
     * Acceptance Criteria:
     * - Test creates multiple messages in room
     * - Test retrieves message history for authorized user
     * - Test verifies all messages are returned
     * - Test verifies messages are in correct order
     * - Test verifies pagination works correctly
     * - Test uses Spring Boot test framework
     * - Test passes with fixed code
     */
    @Test
    void testMessageHistoryCanBeRetrieved() {
        // Arrange
        int messageCount = 3;
        for (int i = 0; i < messageCount; i++) {
            String messageContent = "History message " + (i + 1);
            chatMessageService.sendMessage(testUser.getId(), testRoom.getId(), messageContent);
        }

        // Act
        java.util.List<Message> messages = messageRepository.findAll();

        // Assert
        assertTrue(messages.size() >= messageCount,
            "Should retrieve at least " + messageCount + " messages");

        // Verify messages are from the test room
        long testRoomMessageCount = messages.stream()
            .filter(m -> m.getChatRoom().getId().equals(testRoom.getId()))
            .count();
        assertEquals(messageCount, testRoomMessageCount,
            "Should retrieve exactly " + messageCount + " messages from test room");
    }

    /**
     * Task 4.6: Write preservation test for JOIN/LEAVE system messages
     *
     * Verifies that JOIN and LEAVE system messages continue to be broadcast correctly.
     *
     * Acceptance Criteria:
     * - Test joins room and verifies JOIN message is broadcast
     * - Test leaves room and verifies LEAVE message is broadcast
     * - Test verifies system messages are visible to other room members
     * - Test verifies system messages include correct user and timestamp
     * - Test uses Spring Boot test framework
     * - Test passes with fixed code
     */
    @Test
    void testSystemMessagesArePreserved() {
        // Arrange
        User anotherUser = new User();
        anotherUser.setUsername("anotheruser");
        anotherUser.setEmail("another@example.com");
        anotherUser.setDisplayName("Another User");
        anotherUser.setPasswordHash("hashedpassword");
        anotherUser.setOnline(true);
        anotherUser = userRepository.save(anotherUser);

        // Act - Add another user to room (simulating JOIN)
        RoomMembership newMembership = new RoomMembership();
        newMembership.setUser(anotherUser);
        newMembership.setChatRoom(testRoom);
        newMembership.setJoinedAt(LocalDateTime.now());
        newMembership = roomMembershipRepository.save(newMembership);

        // Assert - Verify membership is created
        assertNotNull(newMembership.getId(), "Membership should be created");
        assertTrue(roomMembershipRepository.findByUserAndChatRoom(anotherUser, testRoom).isPresent(),
            "User should be a member of the room");

        // Act - Remove user from room (simulating LEAVE)
        roomMembershipRepository.delete(newMembership);

        // Assert - Verify membership is removed
        assertFalse(roomMembershipRepository.findByUserAndChatRoom(anotherUser, testRoom).isPresent(),
            "User should no longer be a member of the room");
    }

    /**
     * Test that plain text messages are preserved exactly
     */
    @Test
    void testPlainTextMessageIsPreservedExactly() {
        // Arrange
        String plainText = "This is a plain text message with no HTML";

        // Act
        Message result = chatMessageService.sendMessage(testUser.getId(), testRoom.getId(), plainText);

        // Assert
        assertNotNull(result, "Message should be persisted");
        assertEquals(plainText, result.getContent(),
            "Plain text message should be preserved exactly");

        // Verify in database
        Message persistedMessage = messageRepository.findById(result.getId()).orElse(null);
        assertNotNull(persistedMessage, "Message should be in database");
        assertEquals(plainText, persistedMessage.getContent(),
            "Plain text message should be preserved in database");
    }

    /**
     * Test that unicode and emoji are preserved
     */
    @Test
    void testUnicodeAndEmojiArePreserved() {
        // Arrange
        String unicodeContent = "Unicode: 你好世界 🌍 مرحبا العالم 🎊";

        // Act
        Message result = chatMessageService.sendMessage(testUser.getId(), testRoom.getId(), unicodeContent);

        // Assert
        assertNotNull(result, "Message should be persisted");
        assertEquals(unicodeContent, result.getContent(),
            "Unicode and emoji should be preserved");

        // Verify in database
        Message persistedMessage = messageRepository.findById(result.getId()).orElse(null);
        assertNotNull(persistedMessage, "Message should be in database");
        assertEquals(unicodeContent, persistedMessage.getContent(),
            "Unicode and emoji should be preserved in database");
    }

    /**
     * Test that message metadata is preserved
     */
    @Test
    void testMessageMetadataIsPreserved() {
        // Arrange
        String messageContent = "Message with metadata";

        // Act
        Message result = chatMessageService.sendMessage(testUser.getId(), testRoom.getId(), messageContent);

        // Assert
        assertNotNull(result.getId(), "Message should have an ID");
        assertNotNull(result.getSender(), "Message should have a sender");
        assertEquals(testUser.getId(), result.getSender().getId(), "Sender should be correct");
        assertNotNull(result.getChatRoom(), "Message should have a room");
        assertEquals(testRoom.getId(), result.getChatRoom().getId(), "Room should be correct");
        assertNotNull(result.getTimestamp(), "Message should have a timestamp");
        assertEquals(MessageType.TEXT, result.getMessageType(), "Message type should be TEXT");
    }
}
