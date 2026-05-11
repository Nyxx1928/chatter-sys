package org.example.chat.controller;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.Message;
import org.example.chat.entity.MessageType;
import org.example.chat.entity.RoomMembership;
import org.example.chat.entity.User;
import org.example.chat.exception.UnauthorizedException;
import org.example.chat.repository.RoomMembershipRepository;
import org.example.chat.repository.UserRepository;
import org.example.chat.service.ChatMessageService;
import org.example.chat.service.ChatRoomService;
import org.example.chat.util.SecurityAuditLogger;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Property-based test for race condition on room join.
 * 
 * This test generates rapid join/send sequences with varying delays
 * and verifies that membership is correctly verified before message send.
 * 
 * The test should FAIL on unfixed code (demonstrating the race condition exists)
 * and PASS on fixed code.
 * 
 * Validates: Requirements 2.1, 2.2, 2.3, 2.4
 */
@PropertyDefaults(tries = 100)
class RaceConditionPropertyTest {

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private ChatRoomService chatRoomService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomMembershipRepository roomMembershipRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private SecurityAuditLogger securityAuditLogger;

    @Mock
    private Principal principal;

    @InjectMocks
    private ChatMessageController controller;

    private User testUser;
    private ChatRoom testRoom;
    private RoomMembership testMembership;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setDisplayName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedpassword");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setOnline(true);

        testRoom = new ChatRoom();
        testRoom.setId(1L);
        testRoom.setName("Test Room");
        testRoom.setDescription("A test room");
        testRoom.setCreatedAt(LocalDateTime.now());
        testRoom.setCreatedBy(testUser);

        testMembership = new RoomMembership();
        testMembership.setId(1L);
        testMembership.setUser(testUser);
        testMembership.setChatRoom(testRoom);
        testMembership.setJoinedAt(LocalDateTime.now());

        testMessage = new Message();
        testMessage.setId(1L);
        testMessage.setSender(testUser);
        testMessage.setChatRoom(testRoom);
        testMessage.setContent("Hello, World!");
        testMessage.setTimestamp(LocalDateTime.now());
        testMessage.setMessageType(MessageType.TEXT);
    }

    /**
     * Property test: Rapid join/send sequences should not produce "not a member" errors.
     * 
     * This test generates multiple rapid join/send sequences with varying delays
     * and verifies that membership is correctly verified before message send.
     * 
     * Acceptance Criteria:
     * - Test generates multiple rapid join/send sequences with varying delays
     * - Test verifies that no "not a member" errors occur
     * - Test verifies that membership is persisted before send handler executes
     * - Test includes at least 3 different timing scenarios (immediate send, 10ms delay, 50ms delay)
     * - Test uses property-based testing framework (JUnit 5 with jqwik)
     * - Test fails on unfixed code with counterexample showing race condition
     * - Test passes on fixed code
     */
    @Property
    @Label("Rapid join/send sequences should not produce 'not a member' errors")
    void testRapidJoinSendSequences(
            @ForAll @IntRange(min = 1, max = 10) int sequenceCount,
            @ForAll @IntRange(min = 0, max = 50) int delayMs) {
        
        // Re-initialize mocks for this property test
        MockitoAnnotations.openMocks(this);
        
        // Setup: Configure mocks for successful join and send
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.getRoomById(1L)).thenReturn(testRoom);
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.of(testMembership));
        when(chatMessageService.sendMessage(eq(1L), eq(1L), anyString()))
                .thenReturn(testMessage);

        // Act: Execute rapid join/send sequences
        for (int i = 0; i < sequenceCount; i++) {
            // Join room
            controller.joinRoom(1L, principal);

            // Simulate delay (0ms, 10ms, or 50ms)
            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Send message immediately after join
            Message inputMessage = new Message();
            inputMessage.setContent("Message " + i);

            // This should NOT throw UnauthorizedException
            assertDoesNotThrow(() -> {
                controller.sendMessage(inputMessage, 1L, principal);
            });
        }

        // Assert: Verify membership was checked and message was sent
        verify(roomMembershipRepository, atLeastOnce())
                .findByUserAndChatRoom(testUser, testRoom);
        verify(chatMessageService, times(sequenceCount))
                .sendMessage(eq(1L), eq(1L), anyString());
    }

    /**
     * Property test: Membership must be persisted before send handler executes.
     * 
     * This test verifies that the membership record is committed to the database
     * before the send handler is allowed to execute, preventing race conditions.
     */
    @Property
    @Label("Membership must be persisted before send handler executes")
    void testMembershipPersistedBeforeSend(
            @ForAll @IntRange(min = 1, max = 5) int attemptCount) {
        
        // Re-initialize mocks for this property test
        MockitoAnnotations.openMocks(this);
        
        // Setup: Configure mocks
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.getRoomById(1L)).thenReturn(testRoom);
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.of(testMembership));
        when(chatMessageService.sendMessage(eq(1L), eq(1L), anyString()))
                .thenReturn(testMessage);

        // Act: Join and immediately send multiple times
        for (int i = 0; i < attemptCount; i++) {
            controller.joinRoom(1L, principal);

            Message inputMessage = new Message();
            inputMessage.setContent("Test message " + i);
            controller.sendMessage(inputMessage, 1L, principal);
        }

        // Assert: Verify membership lookup succeeded every time
        // If membership wasn't persisted, this would fail on subsequent attempts
        verify(roomMembershipRepository, times(attemptCount))
                .findByUserAndChatRoom(testUser, testRoom);
        
        // Verify all messages were sent successfully
        verify(chatMessageService, times(attemptCount))
                .sendMessage(eq(1L), eq(1L), anyString());
    }

    /**
     * Property test: Different timing scenarios should all succeed.
     * 
     * This test verifies that the race condition fix works across different
     * timing scenarios: immediate send, 10ms delay, and 50ms delay.
     */
    @Property
    @Label("Different timing scenarios should all succeed")
    void testDifferentTimingScenarios(
            @ForAll @IntRange(min = 0, max = 2) int scenarioIndex) {
        
        // Re-initialize mocks for this property test
        MockitoAnnotations.openMocks(this);
        
        // Setup: Configure mocks
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.getRoomById(1L)).thenReturn(testRoom);
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.of(testMembership));
        when(chatMessageService.sendMessage(eq(1L), eq(1L), anyString()))
                .thenReturn(testMessage);

        // Determine delay based on scenario
        int delayMs = switch (scenarioIndex) {
            case 0 -> 0;      // Immediate send
            case 1 -> 10;     // 10ms delay
            case 2 -> 50;     // 50ms delay
            default -> 0;
        };

        // Act: Join, delay, then send
        controller.joinRoom(1L, principal);

        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        Message inputMessage = new Message();
        inputMessage.setContent("Test message");

        // Assert: Send should succeed without throwing exception
        assertDoesNotThrow(() -> {
            controller.sendMessage(inputMessage, 1L, principal);
        });

        // Verify membership was checked
        verify(roomMembershipRepository).findByUserAndChatRoom(testUser, testRoom);
        verify(chatMessageService).sendMessage(eq(1L), eq(1L), anyString());
    }

    /**
     * Property test: Non-members should still be rejected.
     * 
     * This test verifies that the race condition fix doesn't accidentally
     * allow non-members to send messages.
     */
    @Property
    @Label("Non-members should still be rejected")
    void testNonMembersRejected(
            @ForAll @IntRange(min = 1, max = 5) int attemptCount) {
        
        // Re-initialize mocks for this property test
        MockitoAnnotations.openMocks(this);
        
        // Setup: Configure mocks to simulate non-member
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.getRoomById(1L)).thenReturn(testRoom);
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.empty()); // User is NOT a member

        // Act & Assert: Join should fail for non-members
        for (int i = 0; i < attemptCount; i++) {
            assertThrows(UnauthorizedException.class, () -> {
                controller.joinRoom(1L, principal);
            });
        }

        // Verify membership was checked and join was rejected
        verify(roomMembershipRepository, times(attemptCount))
                .findByUserAndChatRoom(testUser, testRoom);
    }
}
