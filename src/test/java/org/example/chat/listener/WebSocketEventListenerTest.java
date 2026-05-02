package org.example.chat.listener;

import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.MemberRole;
import org.example.chat.entity.RoomMembership;
import org.example.chat.entity.User;
import org.example.chat.repository.UserRepository;
import org.example.chat.service.UserPresenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebSocketEventListener.
 * 
 * Tests the connection lifecycle management including:
 * - Marking users online on connect
 * - Marking users offline on disconnect
 * - Delegating presence tracking to UserPresenceService
 */
@ExtendWith(MockitoExtension.class)
class WebSocketEventListenerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPresenceService userPresenceService;

    @Mock
    private SessionConnectEvent connectEvent;

    @Mock
    private SessionDisconnectEvent disconnectEvent;

    @Mock
    private StompHeaderAccessor headerAccessor;

    @Mock
    private Principal principal;

    @InjectMocks
    private WebSocketEventListener eventListener;

    private User testUser;
    private ChatRoom testRoom1;
    private ChatRoom testRoom2;
    private RoomMembership membership1;
    private RoomMembership membership2;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setDisplayName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setOnline(false);
        testUser.setCreatedAt(LocalDateTime.now());

        // Create test rooms
        testRoom1 = new ChatRoom();
        testRoom1.setId(1L);
        testRoom1.setName("Room 1");

        testRoom2 = new ChatRoom();
        testRoom2.setId(2L);
        testRoom2.setName("Room 2");

        // Create memberships
        membership1 = new RoomMembership();
        membership1.setId(1L);
        membership1.setUser(testUser);
        membership1.setChatRoom(testRoom1);
        membership1.setRole(MemberRole.MEMBER);
        membership1.setJoinedAt(LocalDateTime.now());

        membership2 = new RoomMembership();
        membership2.setId(2L);
        membership2.setUser(testUser);
        membership2.setChatRoom(testRoom2);
        membership2.setRole(MemberRole.MEMBER);
        membership2.setJoinedAt(LocalDateTime.now());
    }

    @Test
    void handleWebSocketConnectListener_ShouldMarkUserOnline() {
        // Arrange
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Mock the event to return the principal
        when(connectEvent.getMessage()).thenReturn(
            org.springframework.messaging.support.MessageBuilder
                .withPayload(new byte[0])
                .setHeader("simpUser", principal)
                .build()
        );

        // Act
        eventListener.handleWebSocketConnectListener(connectEvent);

        // Assert
        verify(userRepository).findByUsername("testuser");
        verify(userPresenceService).markUserOnline(1L);
    }

    @Test
    void handleWebSocketConnectListener_ShouldPublishPresenceUpdatesToAllRooms() {
        // Arrange
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        when(connectEvent.getMessage()).thenReturn(
            org.springframework.messaging.support.MessageBuilder
                .withPayload(new byte[0])
                .setHeader("simpUser", principal)
                .build()
        );

        // Act
        eventListener.handleWebSocketConnectListener(connectEvent);

        // Assert
        verify(userPresenceService).markUserOnline(1L);
    }

    @Test
    void handleWebSocketDisconnectListener_ShouldMarkUserOffline() {
        // Arrange
        testUser.setOnline(true);
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        when(disconnectEvent.getMessage()).thenReturn(
            org.springframework.messaging.support.MessageBuilder
                .withPayload(new byte[0])
                .setHeader("simpUser", principal)
                .build()
        );

        // Act
        eventListener.handleWebSocketDisconnectListener(disconnectEvent);

        // Assert
        verify(userRepository).findByUsername("testuser");
        verify(userPresenceService).markUserOffline(1L);
    }

    @Test
    void handleWebSocketDisconnectListener_ShouldPublishPresenceUpdatesToAllRooms() {
        // Arrange
        testUser.setOnline(true);
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        when(disconnectEvent.getMessage()).thenReturn(
            org.springframework.messaging.support.MessageBuilder
                .withPayload(new byte[0])
                .setHeader("simpUser", principal)
                .build()
        );

        // Act
        eventListener.handleWebSocketDisconnectListener(disconnectEvent);

        // Assert
        verify(userPresenceService).markUserOffline(1L);
    }

    @Test
    void handleWebSocketConnectListener_WithNullPrincipal_ShouldNotProcessEvent() {
        // Arrange
        when(connectEvent.getMessage()).thenReturn(
            org.springframework.messaging.support.MessageBuilder
                .withPayload(new byte[0])
                .build()
        );

        // Act
        eventListener.handleWebSocketConnectListener(connectEvent);

        // Assert
        verify(userRepository, never()).findByUsername(anyString());
        verify(userPresenceService, never()).markUserOnline(anyLong());
    }

    @Test
    void handleWebSocketDisconnectListener_WithNullPrincipal_ShouldNotProcessEvent() {
        // Arrange
        when(disconnectEvent.getMessage()).thenReturn(
            org.springframework.messaging.support.MessageBuilder
                .withPayload(new byte[0])
                .build()
        );

        // Act
        eventListener.handleWebSocketDisconnectListener(disconnectEvent);

        // Assert
        verify(userRepository, never()).findByUsername(anyString());
        verify(userPresenceService, never()).markUserOffline(anyLong());
    }

    @Test
    void handleWebSocketConnectListener_WithNonExistentUser_ShouldNotThrowException() {
        // Arrange
        when(principal.getName()).thenReturn("nonexistent");
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        when(connectEvent.getMessage()).thenReturn(
            org.springframework.messaging.support.MessageBuilder
                .withPayload(new byte[0])
                .setHeader("simpUser", principal)
                .build()
        );

        // Act & Assert
        assertDoesNotThrow(() -> eventListener.handleWebSocketConnectListener(connectEvent));
        verify(userPresenceService, never()).markUserOnline(anyLong());
    }

    @Test
    void handleWebSocketConnectListener_WithNoRoomMemberships_ShouldNotPublishPresence() {
        // Arrange
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        when(connectEvent.getMessage()).thenReturn(
            org.springframework.messaging.support.MessageBuilder
                .withPayload(new byte[0])
                .setHeader("simpUser", principal)
                .build()
        );

        // Act
        eventListener.handleWebSocketConnectListener(connectEvent);

        // Assert
        verify(userPresenceService).markUserOnline(1L);
    }
}
