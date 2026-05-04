package org.example.chat.controller;

import org.example.chat.dto.MessageResponse;
import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.Message;
import org.example.chat.entity.MessageType;
import org.example.chat.entity.User;
import org.example.chat.exception.ErrorResponse;
import org.example.chat.repository.UserRepository;
import org.example.chat.service.ChatMessageService;
import org.example.chat.service.ChatRoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

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
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private Principal principal;

    @InjectMocks
    private ChatMessageController controller;

    private User testUser;
    private ChatRoom testRoom;
    private Message testMessage;

    @BeforeEach
    void setUp() {
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
        when(chatMessageService.sendMessage(eq(1L), eq(1L), anyString())).thenReturn(testMessage);

        Message inputMessage = new Message();
        inputMessage.setContent("Hello, World!");

        // Act
        controller.sendMessage(inputMessage, 1L, principal);

        // Assert
        verify(userRepository).findByUsername("testuser");
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
    void joinRoom_ValidUser_AddsToRoomAndBroadcasts() {
        // Arrange
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.getRoomById(1L)).thenReturn(testRoom);

        // Act
        controller.joinRoom(1L, principal);

        // Assert
        verify(userRepository).findByUsername("testuser");
        verify(chatRoomService).addMember(1L, 1L, null);

        ArgumentCaptor<MessageResponse> messageCaptor = ArgumentCaptor.forClass(MessageResponse.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/room/1"), messageCaptor.capture());

        MessageResponse broadcastedMessage = messageCaptor.getValue();
        assertEquals(MessageType.JOIN, broadcastedMessage.getMessageType());
        assertEquals("Test User joined the room", broadcastedMessage.getContent());
        assertEquals(testUser.getId(), broadcastedMessage.getSenderId());
        assertEquals(testUser.getUsername(), broadcastedMessage.getSenderUsername());
        assertEquals(testUser.getDisplayName(), broadcastedMessage.getSenderDisplayName());
        assertEquals(testRoom.getId(), broadcastedMessage.getChatRoomId());
        assertNotNull(broadcastedMessage.getTimestamp());
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

        verify(chatRoomService, never()).addMember(anyLong(), anyLong(), any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void leaveRoom_ValidUser_RemovesFromRoomAndBroadcasts() {
        // Arrange
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.getRoomById(1L)).thenReturn(testRoom);

        // Act
        controller.leaveRoom(1L, principal);

        // Assert
        verify(userRepository).findByUsername("testuser");
        verify(chatRoomService).removeMember(1L, 1L);

        ArgumentCaptor<MessageResponse> messageCaptor = ArgumentCaptor.forClass(MessageResponse.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/room/1"), messageCaptor.capture());

        MessageResponse broadcastedMessage = messageCaptor.getValue();
        assertEquals(MessageType.LEAVE, broadcastedMessage.getMessageType());
        assertEquals("Test User left the room", broadcastedMessage.getContent());
        assertEquals(testUser.getId(), broadcastedMessage.getSenderId());
        assertEquals(testUser.getUsername(), broadcastedMessage.getSenderUsername());
        assertEquals(testUser.getDisplayName(), broadcastedMessage.getSenderDisplayName());
        assertEquals(testRoom.getId(), broadcastedMessage.getChatRoomId());
        assertNotNull(broadcastedMessage.getTimestamp());
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

        verify(chatRoomService, never()).removeMember(anyLong(), anyLong());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
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
    void joinRoom_ServiceThrowsException_PropagatesException() {
        // Arrange
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        doThrow(new IllegalArgumentException("User is already a member"))
                .when(chatRoomService).addMember(1L, 1L, null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            controller.joinRoom(1L, principal);
        });

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void leaveRoom_ServiceThrowsException_PropagatesException() {
        // Arrange
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        doThrow(new IllegalArgumentException("User is not a member"))
                .when(chatRoomService).removeMember(1L, 1L);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            controller.leaveRoom(1L, principal);
        });

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}
