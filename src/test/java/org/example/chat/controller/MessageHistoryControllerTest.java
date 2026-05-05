package org.example.chat.controller;

import org.example.chat.dto.MessageResponse;
import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.Message;
import org.example.chat.entity.MessageType;
import org.example.chat.entity.RoomMembership;
import org.example.chat.entity.User;
import org.example.chat.exception.RoomNotFoundException;
import org.example.chat.repository.ChatRoomRepository;
import org.example.chat.repository.RoomMembershipRepository;
import org.example.chat.repository.UserRepository;
import org.example.chat.service.ChatMessageService;
import org.example.chat.service.ChatRoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageHistoryControllerTest {

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private ChatRoomService chatRoomService;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private RoomMembershipRepository roomMembershipRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private MessageHistoryController controller;

    private User testUser;
    private ChatRoom testRoom;
    private RoomMembership testMembership;
    private Message testMessage;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
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
        testMessage.setMessageType(MessageType.TEXT);

        // Create pageable
        pageable = PageRequest.of(0, 20);

        // Setup common mocks
        when(userDetails.getUsername()).thenReturn("testuser");
    }

    @Test
    void getMessageHistory_ValidMember_ReturnsMessages() {
        // Arrange
        Page<Message> messagePage = new PageImpl<>(List.of(testMessage), pageable, 1);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.of(testMembership));
        when(chatMessageService.getMessageHistory(1L, pageable)).thenReturn(messagePage);

        // Act
        ResponseEntity<Page<MessageResponse>> response = controller.getMessageHistory(1L, pageable, userDetails);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals(1, response.getBody().getContent().size());

        MessageResponse messageResponse = response.getBody().getContent().get(0);
        assertEquals(testMessage.getId(), messageResponse.getId());
        assertEquals(testMessage.getContent(), messageResponse.getContent());
        assertEquals(testUser.getId(), messageResponse.getSenderId());
        assertEquals(testUser.getUsername(), messageResponse.getSenderUsername());

        verify(userRepository).findByUsername("testuser");
        verify(chatRoomRepository).findById(1L);
        verify(roomMembershipRepository).findByUserAndChatRoom(testUser, testRoom);
        verify(chatMessageService).getMessageHistory(1L, pageable);
    }

    @Test
    void getMessageHistory_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            controller.getMessageHistory(1L, pageable, userDetails);
        });

        verify(userRepository).findByUsername("testuser");
        verify(chatRoomRepository, never()).findById(any());
        verify(roomMembershipRepository, never()).findByUserAndChatRoom(any(), any());
        verify(chatMessageService, never()).getMessageHistory(any(), any());
    }

    @Test
    void getMessageHistory_RoomNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RoomNotFoundException.class, () -> {
            controller.getMessageHistory(1L, pageable, userDetails);
        });

        verify(userRepository).findByUsername("testuser");
        verify(chatRoomRepository).findById(1L);
        verify(roomMembershipRepository, never()).findByUserAndChatRoom(any(), any());
        verify(chatMessageService, never()).getMessageHistory(any(), any());
    }

    @Test
    void getMessageHistory_UserNotMember_AddsMembershipAndReturnsMessages() {
        // Arrange
        Page<Message> messagePage = new PageImpl<>(List.of(testMessage), pageable, 1);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.empty());
        when(chatRoomService.addMember(1L, 1L, null)).thenReturn(testMembership);
        when(chatMessageService.getMessageHistory(1L, pageable)).thenReturn(messagePage);

        // Act
        ResponseEntity<Page<MessageResponse>> response = controller.getMessageHistory(1L, pageable, userDetails);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals(1, response.getBody().getContent().size());

        verify(userRepository).findByUsername("testuser");
        verify(chatRoomRepository).findById(1L);
        verify(roomMembershipRepository).findByUserAndChatRoom(testUser, testRoom);
        verify(chatRoomService).addMember(1L, 1L, null);
        verify(chatMessageService).getMessageHistory(1L, pageable);
    }

    @Test
    void getMessageHistory_EmptyHistory_ReturnsEmptyPage() {
        // Arrange
        Page<Message> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.of(testMembership));
        when(chatMessageService.getMessageHistory(1L, pageable)).thenReturn(emptyPage);

        // Act
        ResponseEntity<Page<MessageResponse>> response = controller.getMessageHistory(1L, pageable, userDetails);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getTotalElements());
        assertTrue(response.getBody().getContent().isEmpty());

        verify(chatMessageService).getMessageHistory(1L, pageable);
    }

    @Test
    void getMessageHistory_PaginationWorks_ReturnsCorrectPage() {
        // Arrange
        Pageable secondPage = PageRequest.of(1, 10);
        Page<Message> messagePage = new PageImpl<>(List.of(testMessage), secondPage, 25);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.of(testMembership));
        when(chatMessageService.getMessageHistory(1L, secondPage)).thenReturn(messagePage);

        // Act
        ResponseEntity<Page<MessageResponse>> response = controller.getMessageHistory(1L, secondPage, userDetails);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(25, response.getBody().getTotalElements());
        assertEquals(1, response.getBody().getContent().size());

        verify(chatMessageService).getMessageHistory(1L, secondPage);
    }
}
