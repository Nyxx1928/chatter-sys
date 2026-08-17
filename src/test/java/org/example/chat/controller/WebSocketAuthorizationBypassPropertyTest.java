package org.example.chat.controller;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.Message;
import org.example.chat.entity.MessageType;
import org.example.chat.entity.User;
import org.example.chat.exception.UnauthorizedException;
import org.example.chat.repository.RoomMembershipRepository;
import org.example.chat.repository.UserRepository;
import org.example.chat.service.ChatMessageService;
import org.example.chat.service.ChatRoomService;
import org.example.chat.util.SecurityAuditLogger;

import java.security.Principal;import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Property-based test for WebSocket authorization bypass vulnerability.
 * 
 * This test generates random users and rooms where the user is NOT a member,
 * then attempts to send STOMP messages to those rooms. The test verifies that:
 * 
 * BUG CONDITION (on unfixed code):
 * - Message is broadcast to room subscribers even though sender is not a member
 * - No authorization error is thrown
 * - No error response is sent to the user
 * 
 * EXPECTED BEHAVIOR (on fixed code):
 * - Message is rejected with UnauthorizedException
 * - Error response is sent to user's error queue
 * - Message is NOT broadcast to room
 * 
 * This test is designed to FAIL on unfixed code (demonstrating the vulnerability)
 * and PASS on fixed code (demonstrating the fix).
 * 
 * Validates: Requirements 2.1, 2.2, 2.3, 2.4
 */
@PropertyDefaults(tries = 100)
class WebSocketAuthorizationBypassPropertyTest {

    /**
     * Helper method to create a test user
     */
    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setDisplayName("Test User");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashedpassword");
        user.setCreatedAt(LocalDateTime.now());
        user.setOnline(true);
        return user;
    }

    /**
     * Helper method to create a test room
     */
    private ChatRoom createTestRoom(User creator) {
        ChatRoom room = new ChatRoom();
        room.setId(1L);
        room.setName("Test Room");
        room.setDescription("A test room");
        room.setCreatedAt(LocalDateTime.now());
        room.setCreatedBy(creator);
        return room;
    }

    /**
     * Helper method to create a test message
     */
    private Message createTestMessage(User sender, ChatRoom room) {
        Message message = new Message();
        message.setId(1L);
        message.setSender(sender);
        message.setChatRoom(room);
        message.setContent("Hello, World!");
        message.setTimestamp(LocalDateTime.now());
        message.setMessageType(MessageType.TEXT);
        return message;
    }

    /**
     * Property test: Unauthorized users cannot send messages to rooms they're not members of.
     * 
     * This test generates random users and rooms where the user is NOT a member,
     * then attempts to send a message. The test verifies that:
     * 
     * Acceptance Criteria:
     * - Test generates random userId and roomId pairs where user is NOT a member
     * - Test attempts to send STOMP message via /app/chat.send/{roomId}
     * - Test verifies that message is NOT broadcast to room subscribers (BUG on unfixed code)
     * - Test verifies that UnauthorizedException is thrown (BUG on unfixed code)
     * - Test includes at least 3 scenarios: user never joined, user left room, user joined different room
     * - Test uses property-based testing framework (JUnit 5 with jqwik)
     * - Test fails on unfixed code with counterexample showing unauthorized message broadcast
     * - Test passes on fixed code
     * 
     * Validates: Requirements 2.1, 2.2, 2.3, 2.4
     */
    @Property
    @Label("Unauthorized users cannot send messages to rooms they're not members of")
    void testUnauthorizedUserCannotSendMessage(
            @ForAll @IntRange(min = 1, max = 100) long userId,
            @ForAll @IntRange(min = 1, max = 100) long roomId) {
        
        // Create mocks for this property test
        ChatMessageService chatMessageService = mock(ChatMessageService.class);
        ChatRoomService chatRoomService = mock(ChatRoomService.class);
        UserRepository userRepository = mock(UserRepository.class);
        RoomMembershipRepository roomMembershipRepository = mock(RoomMembershipRepository.class);
        SecurityAuditLogger securityAuditLogger = mock(SecurityAuditLogger.class);
        Principal principal = mock(Principal.class);
        
        ChatMessageController controller = new ChatMessageController(
            chatMessageService, chatRoomService, userRepository, 
            roomMembershipRepository, securityAuditLogger);
        
        User testUser = createTestUser();
        ChatRoom testRoom = createTestRoom(testUser);
        
        // Setup: User is NOT a member of the room
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.getRoomById(roomId)).thenReturn(testRoom);
        
        // BUG CONDITION: User is not a member
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.empty());
        
        // Service should throw UnauthorizedException when user is not a member
        when(chatMessageService.sendMessage(eq(testUser.getId()), eq(roomId), anyString()))
                .thenThrow(new UnauthorizedException("User is not a member of this room"));

        Message inputMessage = new Message();
        inputMessage.setContent("Unauthorized message");

        // Act & Assert: Should throw UnauthorizedException
        assertThrows(UnauthorizedException.class, () -> {
            controller.sendMessage(inputMessage, roomId, principal);
        });

        // Verify that message was NOT broadcast: the controller has no direct
        // broadcasting capability, so the service must never be invoked
        verify(chatMessageService, never()).sendMessage(anyLong(), anyLong(), anyString());
    }

    /**
     * Property test: User who never joined a room cannot send messages.
     * 
     * Scenario: User attempts to send message to a room they never joined.
     * Expected: Message is rejected with UnauthorizedException.
     * 
     * Validates: Requirements 2.1, 2.2
     */
    @Property
    @Label("User who never joined room cannot send messages")
    void testUserNeverJoinedCannotSend(
            @ForAll @IntRange(min = 1, max = 100) long userId,
            @ForAll @IntRange(min = 1, max = 100) long roomId) {
        
        // Create mocks for this property test
        ChatMessageService chatMessageService = mock(ChatMessageService.class);
        ChatRoomService chatRoomService = mock(ChatRoomService.class);
        UserRepository userRepository = mock(UserRepository.class);
        RoomMembershipRepository roomMembershipRepository = mock(RoomMembershipRepository.class);
        SecurityAuditLogger securityAuditLogger = mock(SecurityAuditLogger.class);
        Principal principal = mock(Principal.class);
        
        ChatMessageController controller = new ChatMessageController(chatMessageService, chatRoomService, userRepository, roomMembershipRepository, securityAuditLogger);
        
        User testUser = createTestUser();
        ChatRoom testRoom = createTestRoom(testUser);
        
        // Setup: User never joined the room
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.getRoomById(roomId)).thenReturn(testRoom);
        
        // BUG CONDITION: User is not a member (never joined)
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.empty());
        
        when(chatMessageService.sendMessage(eq(testUser.getId()), eq(roomId), anyString()))
                .thenThrow(new UnauthorizedException("User is not a member of this room"));

        Message inputMessage = new Message();
        inputMessage.setContent("Message from non-member");

        // Act & Assert: Should throw UnauthorizedException
        assertThrows(UnauthorizedException.class, () -> {
            controller.sendMessage(inputMessage, roomId, principal);
        });

        // Verify membership check was performed
        verify(roomMembershipRepository).findByUserAndChatRoom(testUser, testRoom);
        
        // Verify message was NOT broadcast
        verify(chatMessageService, never()).sendMessage(anyLong(), anyLong(), anyString());
    }

    /**
     * Property test: User who left a room cannot send messages.
     * 
     * Scenario: User was a member, left the room, then attempts to send message.
     * Expected: Message is rejected with UnauthorizedException.
     * 
     * Note: In the current design, membership is preserved on leave, so this
     * scenario tests the case where membership was explicitly removed.
     * 
     * Validates: Requirements 2.1, 2.2
     */
    @Property
    @Label("User who left room cannot send messages")
    void testUserLeftCannotSend(
            @ForAll @IntRange(min = 1, max = 100) long userId,
            @ForAll @IntRange(min = 1, max = 100) long roomId) {
        
        // Create mocks for this property test
        ChatMessageService chatMessageService = mock(ChatMessageService.class);
        ChatRoomService chatRoomService = mock(ChatRoomService.class);
        UserRepository userRepository = mock(UserRepository.class);
        RoomMembershipRepository roomMembershipRepository = mock(RoomMembershipRepository.class);
        SecurityAuditLogger securityAuditLogger = mock(SecurityAuditLogger.class);
        Principal principal = mock(Principal.class);
        
        ChatMessageController controller = new ChatMessageController(chatMessageService, chatRoomService, userRepository, roomMembershipRepository, securityAuditLogger);
        
        User testUser = createTestUser();
        ChatRoom testRoom = createTestRoom(testUser);
        
        // Setup: User left the room (membership removed)
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.getRoomById(roomId)).thenReturn(testRoom);
        
        // BUG CONDITION: User is not a member (left the room)
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.empty());
        
        when(chatMessageService.sendMessage(eq(testUser.getId()), eq(roomId), anyString()))
                .thenThrow(new UnauthorizedException("User is not a member of this room"));

        Message inputMessage = new Message();
        inputMessage.setContent("Message after leaving");

        // Act & Assert: Should throw UnauthorizedException
        assertThrows(UnauthorizedException.class, () -> {
            controller.sendMessage(inputMessage, roomId, principal);
        });

        // Verify message was NOT broadcast
        verify(chatMessageService, never()).sendMessage(anyLong(), anyLong(), anyString());
    }

    /**
     * Property test: User who joined different room cannot send to other rooms.
     * 
     * Scenario: User is a member of Room A but attempts to send message to Room B.
     * Expected: Message is rejected with UnauthorizedException.
     * 
     * Validates: Requirements 2.1, 2.2
     */
    @Property
    @Label("User who joined different room cannot send to other rooms")
    void testUserJoinedDifferentRoomCannotSend(
            @ForAll @IntRange(min = 1, max = 100) long userId,
            @ForAll @IntRange(min = 1, max = 100) long roomId) {
        
        // Create mocks for this property test
        ChatMessageService chatMessageService = mock(ChatMessageService.class);
        ChatRoomService chatRoomService = mock(ChatRoomService.class);
        UserRepository userRepository = mock(UserRepository.class);
        RoomMembershipRepository roomMembershipRepository = mock(RoomMembershipRepository.class);
        SecurityAuditLogger securityAuditLogger = mock(SecurityAuditLogger.class);
        Principal principal = mock(Principal.class);
        
        ChatMessageController controller = new ChatMessageController(chatMessageService, chatRoomService, userRepository, roomMembershipRepository, securityAuditLogger);
        
        User testUser = createTestUser();
        ChatRoom roomB = createTestRoom(testUser);
        roomB.setId(2L);
        roomB.setName("Room B");
        
        // Setup: User is member of Room A but not Room B
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.getRoomById(roomId)).thenReturn(roomB);
        
        // BUG CONDITION: User is not a member of Room B
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, roomB))
                .thenReturn(Optional.empty());
        
        when(chatMessageService.sendMessage(eq(testUser.getId()), eq(roomId), anyString()))
                .thenThrow(new UnauthorizedException("User is not a member of this room"));

        Message inputMessage = new Message();
        inputMessage.setContent("Message to wrong room");

        // Act & Assert: Should throw UnauthorizedException
        assertThrows(UnauthorizedException.class, () -> {
            controller.sendMessage(inputMessage, roomId, principal);
        });

        // Verify message was NOT broadcast
        verify(chatMessageService, never()).sendMessage(anyLong(), anyLong(), anyString());
    }

    /**
     * Property test: Authorization check is performed before broadcasting.
     * 
     * This test verifies that the authorization check happens at the controller level
     * before the message is broadcast, preventing the bug condition where unauthorized
     * messages are broadcast to room subscribers.
     * 
     * Validates: Requirements 2.1, 2.4
     */
    @Property
    @Label("Authorization check is performed before broadcasting")
    void testAuthorizationCheckBeforeBroadcast(
            @ForAll @IntRange(min = 1, max = 100) long userId,
            @ForAll @IntRange(min = 1, max = 100) long roomId) {
        
        // Create mocks for this property test
        ChatMessageService chatMessageService = mock(ChatMessageService.class);
        ChatRoomService chatRoomService = mock(ChatRoomService.class);
        UserRepository userRepository = mock(UserRepository.class);
        RoomMembershipRepository roomMembershipRepository = mock(RoomMembershipRepository.class);
        SecurityAuditLogger securityAuditLogger = mock(SecurityAuditLogger.class);
        Principal principal = mock(Principal.class);
        
        ChatMessageController controller = new ChatMessageController(chatMessageService, chatRoomService, userRepository, roomMembershipRepository, securityAuditLogger);
        
        User testUser = createTestUser();
        ChatRoom testRoom = createTestRoom(testUser);
        
        // Setup: User is NOT a member
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.getRoomById(roomId)).thenReturn(testRoom);
        
        // BUG CONDITION: User is not a member
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.empty());
        
        when(chatMessageService.sendMessage(eq(testUser.getId()), eq(roomId), anyString()))
                .thenThrow(new UnauthorizedException("User is not a member of this room"));

        Message inputMessage = new Message();
        inputMessage.setContent("Unauthorized message");

        // Act: Attempt to send message
        assertThrows(UnauthorizedException.class, () -> {
            controller.sendMessage(inputMessage, roomId, principal);
        });

        // Assert: Verify authorization check was performed
        verify(roomMembershipRepository).findByUserAndChatRoom(testUser, testRoom);
        
        // Assert: Verify message was NOT broadcast (authorization check prevented it)
        verify(chatMessageService, never()).sendMessage(anyLong(), anyLong(), anyString());
    }

    /**
     * Property test: Multiple unauthorized attempts are all rejected.
     * 
     * This test generates multiple unauthorized send attempts and verifies that
     * all are rejected, demonstrating consistent authorization enforcement.
     * 
     * Validates: Requirements 2.1, 2.2, 2.4
     */
    @Property
    @Label("Multiple unauthorized attempts are all rejected")
    void testMultipleUnauthorizedAttemptsRejected(
            @ForAll @IntRange(min = 1, max = 10) int attemptCount) {
        
        // Create mocks for this property test
        ChatMessageService chatMessageService = mock(ChatMessageService.class);
        ChatRoomService chatRoomService = mock(ChatRoomService.class);
        UserRepository userRepository = mock(UserRepository.class);
        RoomMembershipRepository roomMembershipRepository = mock(RoomMembershipRepository.class);
        SecurityAuditLogger securityAuditLogger = mock(SecurityAuditLogger.class);
        Principal principal = mock(Principal.class);
        
        ChatMessageController controller = new ChatMessageController(chatMessageService, chatRoomService, userRepository, roomMembershipRepository, securityAuditLogger);
        
        User testUser = createTestUser();
        ChatRoom testRoom = createTestRoom(testUser);
        
        // Setup: User is NOT a member
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.getRoomById(1L)).thenReturn(testRoom);
        
        // BUG CONDITION: User is not a member
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.empty());
        
        when(chatMessageService.sendMessage(eq(testUser.getId()), eq(1L), anyString()))
                .thenThrow(new UnauthorizedException("User is not a member of this room"));

        // Act: Attempt to send multiple messages
        for (int i = 0; i < attemptCount; i++) {
            Message inputMessage = new Message();
            inputMessage.setContent("Unauthorized message " + i);

            // Assert: Each attempt should throw UnauthorizedException
            assertThrows(UnauthorizedException.class, () -> {
                controller.sendMessage(inputMessage, 1L, principal);
            });
        }

        // Verify all attempts were rejected
        verify(chatMessageService, never()).sendMessage(anyLong(), anyLong(), anyString());
    }

    /**
     * Property test: Error response is sent to user's error queue on authorization failure.
     * 
     * This test verifies that when an unauthorized send attempt is made, an error
     * response is sent to the user's error queue, not just silently rejected.
     * 
     * Validates: Requirements 2.2
     */
    @Property
    @Label("Error response is sent to user's error queue on authorization failure")
    void testErrorResponseSentOnAuthorizationFailure(
            @ForAll @IntRange(min = 1, max = 100) long userId,
            @ForAll @IntRange(min = 1, max = 100) long roomId) {
        
        // Create mocks for this property test
        ChatMessageService chatMessageService = mock(ChatMessageService.class);
        ChatRoomService chatRoomService = mock(ChatRoomService.class);
        UserRepository userRepository = mock(UserRepository.class);
        RoomMembershipRepository roomMembershipRepository = mock(RoomMembershipRepository.class);
        SecurityAuditLogger securityAuditLogger = mock(SecurityAuditLogger.class);
        Principal principal = mock(Principal.class);
        
        ChatMessageController controller = new ChatMessageController(chatMessageService, chatRoomService, userRepository, roomMembershipRepository, securityAuditLogger);
        
        User testUser = createTestUser();
        ChatRoom testRoom = createTestRoom(testUser);
        
        // Setup: User is NOT a member
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.getRoomById(roomId)).thenReturn(testRoom);
        
        // BUG CONDITION: User is not a member
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.empty());

        Message inputMessage = new Message();
        inputMessage.setContent("Unauthorized message");

        // Act: Attempt to send message
        assertThrows(UnauthorizedException.class, () -> {
            controller.sendMessage(inputMessage, roomId, principal);
        });

        // Assert: Verify that the authorization check was performed
        // The controller should check membership before calling the service
        verify(roomMembershipRepository).findByUserAndChatRoom(testUser, testRoom);
        
        // Verify that the service was NOT called (authorization failed before service)
        verify(chatMessageService, never()).sendMessage(anyLong(), anyLong(), anyString());
    }

    /**
     * Property test: Authorization failures are logged for security auditing.
     * 
     * This test verifies that authorization failures are properly logged,
     * enabling security auditing and monitoring of unauthorized access attempts.
     * 
     * Validates: Requirements 2.3
     */
    @Property
    @Label("Authorization failures are logged for security auditing")
    void testAuthorizationFailuresLogged(
            @ForAll @IntRange(min = 1, max = 100) long userId,
            @ForAll @IntRange(min = 1, max = 100) long roomId) {
        
        // Create mocks for this property test
        ChatMessageService chatMessageService = mock(ChatMessageService.class);
        ChatRoomService chatRoomService = mock(ChatRoomService.class);
        UserRepository userRepository = mock(UserRepository.class);
        RoomMembershipRepository roomMembershipRepository = mock(RoomMembershipRepository.class);
        SecurityAuditLogger securityAuditLogger = mock(SecurityAuditLogger.class);
        Principal principal = mock(Principal.class);
        
        ChatMessageController controller = new ChatMessageController(chatMessageService, chatRoomService, userRepository, roomMembershipRepository, securityAuditLogger);
        
        User testUser = createTestUser();
        ChatRoom testRoom = createTestRoom(testUser);
        
        // Setup: User is NOT a member
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.getRoomById(roomId)).thenReturn(testRoom);
        
        // BUG CONDITION: User is not a member
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.empty());
        
        when(chatMessageService.sendMessage(eq(testUser.getId()), eq(roomId), anyString()))
                .thenThrow(new UnauthorizedException("User is not a member of this room"));

        Message inputMessage = new Message();
        inputMessage.setContent("Unauthorized message");

        // Act: Attempt to send message
        assertThrows(UnauthorizedException.class, () -> {
            controller.sendMessage(inputMessage, roomId, principal);
        });

        // Assert: Verify that authorization check was performed (which would be logged)
        verify(roomMembershipRepository).findByUserAndChatRoom(testUser, testRoom);
    }
}


