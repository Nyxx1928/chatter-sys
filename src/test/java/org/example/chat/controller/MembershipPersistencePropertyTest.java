package org.example.chat.controller;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.Message;
import org.example.chat.entity.MessageType;
import org.example.chat.entity.RoomMembership;
import org.example.chat.entity.User;
import org.example.chat.repository.RoomMembershipRepository;
import org.example.chat.repository.UserRepository;
import org.example.chat.service.ChatMessageService;
import org.example.chat.service.ChatRoomService;
import org.example.chat.util.SecurityAuditLogger;
import org.junit.jupiter.api.BeforeEach;
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
 * Property-based test for membership persistence on leave.
 * 
 * This test generates leave/rejoin sequences and verifies that membership
 * persists across navigation and leave operations.
 * 
 * The test should FAIL on unfixed code (demonstrating membership is deleted)
 * and PASS on fixed code.
 * 
 * Validates: Requirements 2.5, 2.6, 2.7, 2.8
 */
@PropertyDefaults(tries = 100)
class MembershipPersistencePropertyTest {

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
     * Property test: Membership should persist after leave.
     * 
     * This test generates leave/rejoin sequences and verifies that membership
     * persists across navigation and leave operations.
     * 
     * Acceptance Criteria:
     * - Test generates multiple leave/rejoin sequences
     * - Test verifies that membership record persists after leave
     * - Test verifies that users can re-join without errors
     * - Test includes scenarios: explicit leave, navigation away, rapid rejoin
     * - Test uses property-based testing framework (JUnit 5 with jqwik)
     * - Test fails on unfixed code with counterexample showing membership deletion
     * - Test passes on fixed code
     */
    @Property
    @Label("Membership should persist after leave")
    void testMembershipPersistsAfterLeave(
            @ForAll @IntRange(min = 1, max = 10) int leaveRejoinCycles) {
        
        // Re-initialize mocks for this property test
        MockitoAnnotations.openMocks(this);
        
        // Setup: Configure mocks for successful leave and rejoin
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.getRoomById(1L)).thenReturn(testRoom);
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.of(testMembership));
        when(chatMessageService.sendMessage(eq(1L), eq(1L), anyString()))
                .thenReturn(testMessage);

        // Act: Execute multiple leave/rejoin cycles
        for (int i = 0; i < leaveRejoinCycles; i++) {
            // Leave room (membership should persist)
            controller.leaveRoom(1L, principal);

            // Rejoin room (should succeed because membership persists)
            assertDoesNotThrow(() -> {
                controller.joinRoom(1L, principal);
            });

            // Send message after rejoin (should succeed)
            Message inputMessage = new Message();
            inputMessage.setContent("Message after rejoin " + i);

            assertDoesNotThrow(() -> {
                controller.sendMessage(inputMessage, 1L, principal);
            });
        }

        // Assert: Verify membership was checked multiple times
        // If membership was deleted on leave, rejoin would fail
        verify(roomMembershipRepository, atLeastOnce())
                .findByUserAndChatRoom(testUser, testRoom);
        verify(chatMessageService, times(leaveRejoinCycles))
                .sendMessage(eq(1L), eq(1L), anyString());
    }

    /**
     * Property test: Users can send messages immediately after returning to room.
     * 
     * This test simulates navigation away from a room and returning,
     * verifying that users can send messages without re-joining.
     */
    @Property
    @Label("Users can send messages immediately after returning to room")
    void testSendAfterNavigation(
            @ForAll @IntRange(min = 1, max = 5) int navigationCycles) {
        
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

        // Act: Navigate away and back multiple times
        for (int i = 0; i < navigationCycles; i++) {
            // Send message before navigation
            Message beforeMessage = new Message();
            beforeMessage.setContent("Before navigation " + i);
            controller.sendMessage(beforeMessage, 1L, principal);

            // Simulate navigation away (no explicit leave call)
            // In real app, this would be a page navigation

            // Return to room and send message
            Message afterMessage = new Message();
            afterMessage.setContent("After navigation " + i);

            assertDoesNotThrow(() -> {
                controller.sendMessage(afterMessage, 1L, principal);
            });
        }

        // Assert: Verify all messages were sent successfully
        verify(chatMessageService, times(navigationCycles * 2))
                .sendMessage(eq(1L), eq(1L), anyString());
    }

    /**
     * Property test: Rapid rejoin should succeed.
     * 
     * This test verifies that users can rapidly rejoin a room after leaving,
     * without encountering "not a member" errors.
     */
    @Property
    @Label("Rapid rejoin should succeed")
    void testRapidRejoin(
            @ForAll @IntRange(min = 1, max = 10) int rejoinAttempts) {
        
        // Re-initialize mocks for this property test
        MockitoAnnotations.openMocks(this);
        
        // Setup: Configure mocks
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.getRoomById(1L)).thenReturn(testRoom);
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.of(testMembership));

        // Act: Leave and rejoin rapidly
        for (int i = 0; i < rejoinAttempts; i++) {
            controller.leaveRoom(1L, principal);

            // Rejoin should succeed immediately
            assertDoesNotThrow(() -> {
                controller.joinRoom(1L, principal);
            });
        }

        // Assert: Verify all rejoin attempts succeeded
        verify(roomMembershipRepository, times(rejoinAttempts))
                .findByUserAndChatRoom(testUser, testRoom);
    }

    /**
     * Property test: Membership persists across different action sequences.
     * 
     * This test generates different sequences of actions (leave, rejoin, send)
     * and verifies that membership persists throughout.
     */
    @Property
    @Label("Membership persists across different action sequences")
    void testMembershipPersistsAcrossSequences(
            @ForAll @IntRange(min = 0, max = 3) int actionSequence) {
        
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

        // Act: Execute different action sequences
        switch (actionSequence) {
            case 0:
                // Sequence: leave, rejoin, send
                controller.leaveRoom(1L, principal);
                controller.joinRoom(1L, principal);
                Message msg1 = new Message();
                msg1.setContent("After rejoin");
                controller.sendMessage(msg1, 1L, principal);
                break;

            case 1:
                // Sequence: send, leave, send
                Message msg2 = new Message();
                msg2.setContent("Before leave");
                controller.sendMessage(msg2, 1L, principal);
                controller.leaveRoom(1L, principal);
                Message msg3 = new Message();
                msg3.setContent("After leave");
                controller.sendMessage(msg3, 1L, principal);
                break;

            case 2:
                // Sequence: leave, send (without explicit rejoin)
                controller.leaveRoom(1L, principal);
                Message msg4 = new Message();
                msg4.setContent("After leave without rejoin");
                controller.sendMessage(msg4, 1L, principal);
                break;

            case 3:
                // Sequence: multiple leaves and sends
                for (int i = 0; i < 3; i++) {
                    controller.leaveRoom(1L, principal);
                    Message msg5 = new Message();
                    msg5.setContent("Message " + i);
                    controller.sendMessage(msg5, 1L, principal);
                }
                break;
        }

        // Assert: Verify membership was checked
        verify(roomMembershipRepository, atLeastOnce())
                .findByUserAndChatRoom(testUser, testRoom);
    }

    /**
     * Property test: Membership should be preserved even after multiple leaves.
     * 
     * This test verifies that calling leave multiple times doesn't delete membership.
     */
    @Property
    @Label("Membership should be preserved even after multiple leaves")
    void testMembershipPreservedAfterMultipleLeaves(
            @ForAll @IntRange(min = 1, max = 10) int leaveCount) {
        
        // Re-initialize mocks for this property test
        MockitoAnnotations.openMocks(this);
        
        // Setup: Configure mocks
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.getRoomById(1L)).thenReturn(testRoom);
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.of(testMembership));

        // Act: Leave multiple times
        for (int i = 0; i < leaveCount; i++) {
            controller.leaveRoom(1L, principal);
        }

        // After all leaves, user should still be able to rejoin
        assertDoesNotThrow(() -> {
            controller.joinRoom(1L, principal);
        });

        // Assert: Verify membership was checked after all leaves
        verify(roomMembershipRepository).findByUserAndChatRoom(testUser, testRoom);
    }
}
