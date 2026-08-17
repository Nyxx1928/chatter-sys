package org.example.chat.service;

import org.example.chat.dto.MessageResponse;
import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.Message;
import org.example.chat.entity.MessageType;
import org.example.chat.entity.RoomMembership;
import org.example.chat.entity.User;
import org.example.chat.exception.UnauthorizedException;
import org.example.chat.repository.ChatRoomRepository;
import org.example.chat.repository.MessageRepository;
import org.example.chat.repository.RoomMembershipRepository;
import org.example.chat.repository.UserRepository;
import org.example.chat.util.HtmlSanitizer;
import org.example.chat.util.SecurityAuditLogger;
import org.example.chat.service.PushNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomMembershipRepository roomMembershipRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private HtmlSanitizer htmlSanitizer;

    @Mock
    private SecurityAuditLogger securityAuditLogger;

    @Mock
    private PushNotificationService pushNotificationService;

    private ChatMessageService chatMessageService;

    private User testUser;
    private ChatRoom testRoom;
    private RoomMembership testMembership;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        chatMessageService = new ChatMessageService(
            messageRepository, chatRoomRepository, userRepository,
            roomMembershipRepository, messagingTemplate, htmlSanitizer, securityAuditLogger,
            pushNotificationService
        );

        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setDisplayName("Test User");

        // Create test room
        testRoom = new ChatRoom();
        testRoom.setId(1L);
        testRoom.setName("Test Room");
        testRoom.setCreatedAt(LocalDateTime.now());

        // Create test membership
        testMembership = new RoomMembership();
        testMembership.setId(1L);
        testMembership.setUser(testUser);
        testMembership.setChatRoom(testRoom);
        testMembership.setJoinedAt(LocalDateTime.now());

        // Create test message
        testMessage = new Message();
        testMessage.setId(1L);
        testMessage.setSender(testUser);
        testMessage.setChatRoom(testRoom);
        testMessage.setContent("Test message");
        testMessage.setTimestamp(LocalDateTime.now());

        // Setup default mock behavior for sanitizer (lenient to avoid unnecessary stubbing errors)
        lenient().when(htmlSanitizer.sanitize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(htmlSanitizer.containsDangerousPatterns(anyString())).thenReturn(false);
    }

    @Test
    void sendMessage_ValidMessage_PersistsAndBroadcasts() {
        // Arrange
        String content = "Hello, world!";
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.of(testMembership));
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        // Act
        Message result = chatMessageService.sendMessage(1L, 1L, content);

        // Assert
        assertNotNull(result);
        assertEquals(testMessage.getId(), result.getId());
        verify(messageRepository).save(any(Message.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/room/1"), any(MessageResponse.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/rooms"), any(MessageResponse.class));
    }

    @Test
    void sendMessage_EmptyContent_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            chatMessageService.sendMessage(1L, 1L, "");
        });

        verify(messageRepository, never()).save(any(Message.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void sendMessage_NullContent_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            chatMessageService.sendMessage(1L, 1L, null);
        });

        verify(messageRepository, never()).save(any(Message.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void sendMessage_ContentTooLong_ThrowsException() {
        // Arrange
        String longContent = "a".repeat(5001);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            chatMessageService.sendMessage(1L, 1L, longContent);
        });

        verify(messageRepository, never()).save(any(Message.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void sendMessage_SenderNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            chatMessageService.sendMessage(1L, 1L, "Test message");
        });

        assertEquals("Sender not found", exception.getMessage());
        verify(messageRepository, never()).save(any(Message.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void sendMessage_RoomNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            chatMessageService.sendMessage(1L, 1L, "Test message");
        });

        assertEquals("Chat room not found", exception.getMessage());
        verify(messageRepository, never()).save(any(Message.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void sendMessage_UserNotMember_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.empty());

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            chatMessageService.sendMessage(1L, 1L, "Test message");
        });

        assertEquals("User is not a member of this chat room", exception.getMessage());
        verify(messageRepository, never()).save(any(Message.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void sendMessage_ValidMessage_BroadcastsToCorrectTopic() {
        // Arrange
        String content = "Hello, world!";
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.of(testMembership));
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        // Act
        chatMessageService.sendMessage(1L, 1L, content);

        // Assert
        verify(messagingTemplate).convertAndSend(eq("/topic/room/1"), any(MessageResponse.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/rooms"), any(MessageResponse.class));
    }

    @Test
    void getMessageHistory_ValidRoom_ReturnsMessages() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Message> expectedPage = new PageImpl<>(List.of(testMessage), pageable, 1);
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(messageRepository.findByChatRoomOrderByTimestampDesc(testRoom, pageable))
                .thenReturn(expectedPage);

        // Act
        Page<Message> result = chatMessageService.getMessageHistory(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testMessage.getId(), result.getContent().get(0).getId());
        verify(messageRepository).findByChatRoomOrderByTimestampDesc(testRoom, pageable);
    }

    @Test
    void getMessageHistory_RoomNotFound_ThrowsException() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            chatMessageService.getMessageHistory(1L, pageable);
        });

        assertEquals("Chat room not found", exception.getMessage());
        verify(messageRepository, never()).findByChatRoomOrderByTimestampDesc(any(), any());
    }

    @Test
    void getMessageHistory_EmptyRoom_ReturnsEmptyPage() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Message> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(messageRepository.findByChatRoomOrderByTimestampDesc(testRoom, pageable))
                .thenReturn(emptyPage);

        // Act
        Page<Message> result = chatMessageService.getMessageHistory(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void sendMessage_ValidMessage_SetsTimestamp() {
        // Arrange
        String content = "Hello, world!";
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.of(testMembership));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(1L);
            return msg;
        });

        // Act
        Message result = chatMessageService.sendMessage(1L, 1L, content);

        // Assert
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        Message savedMessage = messageCaptor.getValue();
        assertNotNull(savedMessage.getTimestamp());
        assertEquals(testUser, savedMessage.getSender());
        assertEquals(testRoom, savedMessage.getChatRoom());
        assertEquals(content, savedMessage.getContent());
    }

    @Test
    void sendSystemMessage_ValidMember_PersistsWithTypeAndBroadcasts() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.of(testMembership));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(1L);
            return msg;
        });

        // Act
        Message result = chatMessageService.sendSystemMessage(1L, 1L, MessageType.JOIN,
                "Test User joined the room");

        // Assert
        assertNotNull(result);
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertEquals(MessageType.JOIN, messageCaptor.getValue().getMessageType());
        verify(messagingTemplate).convertAndSend(eq("/topic/room/1"), any(MessageResponse.class));
        // System messages must not update global previews
        verify(messagingTemplate, never()).convertAndSend(eq("/topic/rooms"), any(MessageResponse.class));
    }

    @Test
    void sendSystemMessage_UserNotMember_Throws() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> {
            chatMessageService.sendSystemMessage(1L, 1L, MessageType.LEAVE, "Test User left the room");
        });

        verify(messageRepository, never()).save(any(Message.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void sendSystemMessage_NullType_Throws() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            chatMessageService.sendSystemMessage(1L, 1L, null, "Some content");
        });

        verify(messageRepository, never()).save(any(Message.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}

