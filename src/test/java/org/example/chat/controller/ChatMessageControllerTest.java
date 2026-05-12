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
import org.mockito.ArgumentCaptor;
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
    private RoomMembershipRepository roomMembershipRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

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
            roomMembershipRepository, messagingTemplate, securityAuditLogger
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
    void joinRoom_ValidUser_AddsToRoomAndBroadcasts() {
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
        // addMember is no longer called — joinRoom only broadcasts for existing members
        verify(chatRoomService, never()).addMember(anyLong(), anyLong(), any());

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
    void leaveRoom_ValidUser_BroadcastsAndPreservesMembership() {
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
        verify(chatRoomService, never()).removeMember(anyLong(), anyLong());

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

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
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

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
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

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}
