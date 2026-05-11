package org.example.chat.integration;

import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.Message;
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
 * Fix checking test for XSS sanitization on persistence.
 * Verifies that message content is sanitized before persistence.
 *
 * Validates: Requirements 2.5, 2.6, 2.7, 2.8
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class XssSanitizationFixIT {

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

        // Add user to room membership
        RoomMembership membership = new RoomMembership();
        membership.setUser(testUser);
        membership.setChatRoom(testRoom);
        membership.setJoinedAt(LocalDateTime.now());
        roomMembershipRepository.save(membership);
    }

    /**
     * Test 3.2: Write fix checking test for XSS sanitization on persistence
     *
     * Verifies that message content is sanitized before persistence.
     *
     * Acceptance Criteria:
     * - Test sends message with XSS payload: <script>alert('xss')</script>
     * - Test verifies message is persisted
     * - Test verifies persisted content is escaped: &lt;script&gt;alert('xss')&lt;/script&gt;
     * - Test verifies original payload is NOT in database
     * - Test verifies no scripts execute
     * - Test uses Spring Boot test framework
     * - Test passes with fixed code
     */
    @Test
    void testXssPayloadIsSanitized() {
        // Arrange
        String xssPayload = "<script>alert('xss')</script>";

        // Act
        Message result = chatMessageService.sendMessage(testUser.getId(), testRoom.getId(), xssPayload);

        // Assert message is persisted
        assertNotNull(result, "Message should be persisted");
        assertNotNull(result.getId(), "Message should have an ID");

        // Assert persisted content is escaped (HTML entities)
        String sanitizedContent = result.getContent();
        assertTrue(sanitizedContent.contains("&lt;script&gt;"),
            "Message content should have escaped script tag");
        assertTrue(sanitizedContent.contains("&lt;/script&gt;"),
            "Message content should have escaped closing script tag");

        // Retrieve message from database to verify persistence
        Message persistedMessage = messageRepository.findById(result.getId()).orElse(null);
        assertNotNull(persistedMessage, "Message should be persisted in database");

        // Assert persisted content is escaped
        String persistedContent = persistedMessage.getContent();
        assertTrue(persistedContent.contains("&lt;script&gt;"),
            "Persisted content should have escaped script tag");

        // Assert original payload is NOT in database
        assertFalse(persistedContent.contains("<script>"),
            "Original XSS payload should not be in database");

        // Assert dangerous patterns are not present
        assertFalse(persistedContent.contains("<script>"),
            "Persisted content should not contain <script> tag");
        // Note: alert( may still be present but escaped, which is safe
    }

    /**
     * Test that various XSS payloads are sanitized
     */
    @Test
    void testMultipleXssPayloadsAreSanitized() {
        // Test cases: [payload, expectedPattern]
        String[][] testCases = {
            {"<img src=x onerror=\"alert('xss')\">", "&lt;img"},
            {"<svg onload=\"fetch('http://attacker.com')\">", "&lt;svg"},
            {"<iframe src=\"javascript:alert('xss')\"></iframe>", "&lt;iframe"},
            {"<body onload=\"alert('xss')\">", "&lt;body"},
        };

        for (String[] testCase : testCases) {
            String payload = testCase[0];
            String expectedPattern = testCase[1];

            // Act
            Message result = chatMessageService.sendMessage(testUser.getId(), testRoom.getId(), payload);

            // Assert
            assertNotNull(result, "Message should be persisted for payload: " + payload);
            assertTrue(result.getContent().contains(expectedPattern),
                "Content should be escaped for payload: " + payload);

            // Verify in database
            Message persistedMessage = messageRepository.findById(result.getId()).orElse(null);
            assertNotNull(persistedMessage, "Message should be in database for payload: " + payload);
            assertTrue(persistedMessage.getContent().contains(expectedPattern),
                "Persisted content should be escaped for payload: " + payload);
            
            // Verify original dangerous pattern is not present
            assertFalse(persistedMessage.getContent().contains(payload),
                "Original payload should not be in database: " + payload);
        }
    }

    /**
     * Test that legitimate content is preserved (preservation test)
     */
    @Test
    void testLegitimateContentIsPreserved() {
        // Arrange
        String legitimateContent = "Hello <world> & friends! 🎉";

        // Act
        Message result = chatMessageService.sendMessage(testUser.getId(), testRoom.getId(), legitimateContent);

        // Assert
        assertNotNull(result, "Message should be persisted");
        
        // Verify HTML entities are escaped
        assertTrue(result.getContent().contains("&lt;world&gt;"),
            "Angle brackets should be escaped");
        assertTrue(result.getContent().contains("&amp;"),
            "Ampersand should be escaped");
        assertTrue(result.getContent().contains("🎉"),
            "Emoji should be preserved");
    }

    /**
     * Test that plain text messages are preserved exactly
     */
    @Test
    void testPlainTextMessageIsPreserved() {
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
     * Test that special characters are preserved
     */
    @Test
    void testSpecialCharactersArePreserved() {
        // Arrange
        String specialChars = "Special chars: @#$%^&*()_+-=[]{}|;:',.<>?/~`";

        // Act
        Message result = chatMessageService.sendMessage(testUser.getId(), testRoom.getId(), specialChars);

        // Assert
        assertNotNull(result, "Message should be persisted");
        // Verify that dangerous characters are escaped
        assertTrue(result.getContent().contains("&amp;"),
            "Ampersand should be escaped");
        // Other special characters should be preserved or safely escaped
        assertTrue(result.getContent().contains("@#$%^"),
            "Safe special characters should be preserved");
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
}
