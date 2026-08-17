package org.example.chat.controller;

import org.example.chat.dto.MessageResponse;
import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.Message;
import org.example.chat.entity.MessageType;
import org.example.chat.entity.RoomMembership;
import org.example.chat.entity.User;
import org.example.chat.exception.ErrorResponse;
import org.example.chat.exception.UnauthorizedException;
import org.example.chat.repository.RoomMembershipRepository;
import org.example.chat.repository.UserRepository;
import org.example.chat.service.ChatMessageService;
import org.example.chat.service.ChatRoomService;
import org.example.chat.util.SecurityAuditLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageControllerTest {

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private ChatRoomService chatRoomService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomMembershipRepository roomMembershipRepository;

    @Mock
    private SecurityAuditLogger securityAuditLogger;

    @Mock
    private Principal principal;

    private ChatMessageController controller;

    private User testUser;
    private ChatRoom testRoom;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        controller = new ChatMessageController(
            chatMessageService, chatRoomService, userRepository,
            roomMembershipRepository, securityAuditLogger
        );

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

        testMessage = new Message();
        testMessage.setId(1L);
        testMessage.setSender(testUser);
        testMessage.setChatRoom(testRoom);
        testMessage.setContent("Hello, World!");
        testMessage.setTimestamp(LocalDateTime.now());
        testMessage.setMessageType(MessageType.TEXT);
    }

    @Test
    void sendMessage_ValidMessage_DelegatesToService() {
        // Arrange
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.getRoomById(1L)).thenReturn(testRoom);
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.of(new RoomMembership())); // User is a member
        when(chatMessageService.sendMessage(eq(1L), eq(1L), anyString())).thenReturn(testMessage);

        Message inputMessage = new Message();
        inputMessage.setContent("Hello, World!");

        // Act
        controller.sendMessage(inputMessage, 1L, principal);

        // Assert
        verify(userRepository).findByUsername("testuser");
        verify(chatRoomService).getRoomById(1L);
        verify(roomMembershipRepository).findByUserAndChatRoom(testUser, testRoom);
        verify(chatMessageService).sendMessage(1L, 1L, "Hello, World!");
    }

    @Test
    void sendMessage_UserNotFound_ThrowsException() {
        // Arrange
        when(principal.getName()).thenReturn("nonexistent");
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        Message inputMessage = new Message();
        inputMessage.setContent("Hello, World!");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            controller.sendMessage(inputMessage, 1L, principal);
        });

        verify(chatMessageService, never()).sendMessage(anyLong(), anyLong(), anyString());
    }

    @Test
    void joinRoom_ValidUser_PersistsAndBroadcastsViaService() {
        // Arrange
        RoomMembership membership = new RoomMembership();
        membership.setUser(testUser);
        membership.setChatRoom(testRoom);

        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.getRoomById(1L)).thenReturn(testRoom);
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.of(membership));

        // Act
        controller.joinRoom(1L, principal);

        // Assert
        verify(userRepository).findByUsername("testuser");
        verify(chatRoomService).getRoomById(1L);
        verify(roomMembershipRepository).findByUserAndChatRoom(testUser, testRoom);
        // addMember is no longer called — joinRoom only announces presence for existing members
        verify(chatRoomService, never()).addMember(anyLong(), anyLong(), any(), anyLong());

        // JOIN system message is persisted and broadcast via the service
        verify(chatMessageService).sendSystemMessage(eq(1L), eq(1L), eq(MessageType.JOIN),
                eq("Test User joined the room"));
    }

    @Test
    void joinRoom_UserNotFound_ThrowsException() {
        // Arrange
        when(principal.getName()).thenReturn("nonexistent");
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            controller.joinRoom(1L, principal);
        });

        verify(chatRoomService, never()).addMember(anyLong(), anyLong(), any(), anyLong());
        verify(chatMessageService, never()).sendSystemMessage(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void leaveRoom_ValidUser_PersistsViaServiceAndPreservesMembership() {
        // Arrange
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.getRoomById(1L)).thenReturn(testRoom);
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.of(new RoomMembership())); // User is a member

        // Act
        controller.leaveRoom(1L, principal);

        // Assert
        verify(userRepository).findByUsername("testuser");
        verify(chatRoomService, never()).removeMember(anyLong(), anyLong(), anyLong());

        // LEAVE system message is persisted and broadcast via the service
        verify(chatMessageService).sendSystemMessage(eq(1L), eq(1L), eq(MessageType.LEAVE),
                eq("Test User left the room"));
    }

    @Test
    void leaveRoom_UserNotFound_ThrowsException() {
        // Arrange
        when(principal.getName()).thenReturn("nonexistent");
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            controller.leaveRoom(1L, principal);
        });

        verify(chatRoomService, never()).removeMember(anyLong(), anyLong(), anyLong());
        verify(chatMessageService, never()).sendSystemMessage(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void handleException_WithPrincipal_ReturnsErrorResponse() {
        // Arrange
        Exception testException = new RuntimeException("Test error");
        when(principal.getName()).thenReturn("testuser");

        // Act
        ErrorResponse response = controller.handleException(testException, principal);

        // Assert
        assertNotNull(response);
        assertEquals("Test error", response.getMessage());
        assertEquals(500, response.getStatus());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void handleException_WithoutPrincipal_ReturnsErrorResponse() {
        // Arrange
        Exception testException = new RuntimeException("Test error");

        // Act
        ErrorResponse response = controller.handleException(testException, null);

        // Assert
        assertNotNull(response);
        assertEquals("Test error", response.getMessage());
        assertEquals(500, response.getStatus());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void joinRoom_UserNotMember_ThrowsUnauthorizedException() {
        // Arrange
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.getRoomById(1L)).thenReturn(testRoom);
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> {
            controller.joinRoom(1L, principal);
        });

        verify(chatMessageService, never()).sendSystemMessage(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void joinRoom_ServiceThrowsException_PropagatesException() {
        // Arrange
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.getRoomById(1L))
                .thenThrow(new IllegalArgumentException("Room not found"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            controller.joinRoom(1L, principal);
        });

        verify(chatMessageService, never()).sendSystemMessage(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void leaveRoom_ServiceThrowsException_PropagatesException() {
        // Arrange
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.getRoomById(1L)).thenThrow(new IllegalArgumentException("Room not found"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            controller.leaveRoom(1L, principal);
        });

        verify(chatMessageService, never()).sendSystemMessage(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void handleValidationException_ReturnsBadRequestErrorResponse() {
        // Arrange
        IllegalArgumentException testException = new IllegalArgumentException("Message content cannot be empty");
        when(principal.getName()).thenReturn("testuser");

        // Act
        ErrorResponse response = controller.handleValidationException(testException, principal);

        // Assert
        assertNotNull(response);
        assertEquals("Message content cannot be empty", response.getMessage());
        assertEquals(400, response.getStatus());
        assertNotNull(response.getTimestamp());
    }
}
