package org.example.chat.service;

import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.MemberRole;
import org.example.chat.entity.RoomMembership;
import org.example.chat.entity.User;
import org.example.chat.exception.RoomNotFoundException;
import org.example.chat.exception.UnauthorizedException;
import org.example.chat.exception.UserNotFoundException;
import org.example.chat.repository.ChatRoomRepository;
import org.example.chat.repository.RoomMembershipRepository;
import org.example.chat.repository.UserRepository;
import org.example.chat.service.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private RoomMembershipRepository roomMembershipRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RateLimiterService rateLimiterService;

    @InjectMocks
    private ChatRoomService chatRoomService;

    private User testUser;
    private ChatRoom testRoom;
    private RoomMembership testMembership;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setDisplayName("Test User");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setOnline(false);

        testRoom = new ChatRoom();
        testRoom.setId(1L);
        testRoom.setName("Test Room");
        testRoom.setDescription("A test chat room");
        testRoom.setCreatedAt(LocalDateTime.now());
        testRoom.setCreatedBy(testUser);

        testMembership = new RoomMembership();
        testMembership.setId(1L);
        testMembership.setUser(testUser);
        testMembership.setChatRoom(testRoom);
        testMembership.setJoinedAt(LocalDateTime.now());
        testMembership.setRole(MemberRole.OWNER);
    }

    @Test
    void createRoom_ValidInput_CreatesRoomAndAddsOwner() {
        // Arrange
        String roomName = "New Room";
        String description = "New room description";
        Long creatorId = 1L;

        when(chatRoomRepository.findByName(roomName)).thenReturn(Optional.empty());
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(testUser));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(testRoom);
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(roomMembershipRepository.findByUserAndChatRoom(any(User.class), any(ChatRoom.class)))
                .thenReturn(Optional.empty());
        when(roomMembershipRepository.save(any(RoomMembership.class))).thenReturn(testMembership);

        // Act
        ChatRoom result = chatRoomService.createRoom(roomName, description, creatorId);

        // Assert
        assertNotNull(result);
        verify(chatRoomRepository).save(any(ChatRoom.class));
        verify(roomMembershipRepository).save(any(RoomMembership.class));
    }

    @Test
    void createRoom_DuplicateName_ThrowsException() {
        // Arrange
        String roomName = "Existing Room";
        String description = "Description";
        Long creatorId = 1L;

        when(chatRoomRepository.findByName(roomName)).thenReturn(Optional.of(testRoom));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chatRoomService.createRoom(roomName, description, creatorId));
        assertEquals("Room name already exists", exception.getMessage());
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    @Test
    void createRoom_EmptyName_ThrowsException() {
        // Arrange
        String roomName = "";
        String description = "Description";
        Long creatorId = 1L;

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chatRoomService.createRoom(roomName, description, creatorId));
        assertEquals("Room name cannot be empty", exception.getMessage());
    }

    @Test
    void createRoom_NameTooLong_ThrowsException() {
        // Arrange
        String roomName = "a".repeat(101);
        String description = "Description";
        Long creatorId = 1L;

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chatRoomService.createRoom(roomName, description, creatorId));
        assertEquals("Room name cannot exceed 100 characters", exception.getMessage());
    }

    @Test
    void createRoom_CreatorNotFound_ThrowsException() {
        // Arrange
        String roomName = "New Room";
        String description = "Description";
        Long creatorId = 999L;

        when(chatRoomRepository.findByName(roomName)).thenReturn(Optional.empty());
        when(userRepository.findById(creatorId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chatRoomService.createRoom(roomName, description, creatorId));
        assertEquals("Creator user not found", exception.getMessage());
    }

    @Test
    void getRoomById_ExistingRoom_ReturnsRoom() {
        // Arrange
        Long roomId = 1L;
        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));

        // Act
        ChatRoom result = chatRoomService.getRoomById(roomId);

        // Assert
        assertNotNull(result);
        assertEquals(testRoom.getId(), result.getId());
        assertEquals(testRoom.getName(), result.getName());
    }

    @Test
    void getRoomById_NonExistingRoom_ThrowsException() {
        // Arrange
        Long roomId = 999L;
        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.empty());

        // Act & Assert
        RoomNotFoundException exception = assertThrows(
                RoomNotFoundException.class,
                () -> chatRoomService.getRoomById(roomId));
        assertTrue(exception.getMessage().contains("999"));
    }

    @Test
    void listRooms_ReturnsAllRooms() {
        // Arrange
        ChatRoom room2 = new ChatRoom();
        room2.setId(2L);
        room2.setName("Room 2");
        List<ChatRoom> rooms = Arrays.asList(testRoom, room2);

        when(chatRoomRepository.findAll()).thenReturn(rooms);

        // Act
        List<ChatRoom> result = chatRoomService.listRooms();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(chatRoomRepository).findAll();
    }

    @Test
    void getRoomMembers_ExistingRoom_ReturnsMembers() {
        // Arrange
        Long roomId = 1L;
        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");

        RoomMembership membership2 = new RoomMembership();
        membership2.setUser(user2);
        membership2.setChatRoom(testRoom);

        List<RoomMembership> memberships = Arrays.asList(testMembership, membership2);

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(roomMembershipRepository.findByChatRoom(testRoom)).thenReturn(memberships);

        // Act
        List<User> result = chatRoomService.getRoomMembers(roomId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(testUser));
        assertTrue(result.contains(user2));
    }

    @Test
    void getRoomMembers_NonExistingRoom_ThrowsException() {
        // Arrange
        Long roomId = 999L;
        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                RoomNotFoundException.class,
                () -> chatRoomService.getRoomMembers(roomId));
    }

    @Test
    void addMember_ValidInput_CreatesMembership() {
        // Arrange
        Long roomId = 1L;
        Long userId = 2L;
        User newUser = new User();
        newUser.setId(userId);
        newUser.setUsername("newuser");

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(userId)).thenReturn(Optional.of(newUser));
        when(roomMembershipRepository.findByUserAndChatRoom(newUser, testRoom))
                .thenReturn(Optional.empty());
        when(roomMembershipRepository.save(any(RoomMembership.class))).thenReturn(testMembership);

        // Act
        RoomMembership result = chatRoomService.addMember(roomId, userId, MemberRole.MEMBER);

        // Assert
        assertNotNull(result);
        verify(roomMembershipRepository).save(any(RoomMembership.class));
    }

    @Test
    void addMember_NullRole_DefaultsToMember() {
        // Arrange
        Long roomId = 1L;
        Long userId = 2L;
        User newUser = new User();
        newUser.setId(userId);

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(userId)).thenReturn(Optional.of(newUser));
        when(roomMembershipRepository.findByUserAndChatRoom(newUser, testRoom))
                .thenReturn(Optional.empty());
        when(roomMembershipRepository.save(any(RoomMembership.class))).thenAnswer(invocation -> {
            RoomMembership membership = invocation.getArgument(0);
            assertEquals(MemberRole.MEMBER, membership.getRole());
            return membership;
        });

        // Act
        chatRoomService.addMember(roomId, userId, null);

        // Assert
        verify(roomMembershipRepository).save(any(RoomMembership.class));
    }

    @Test
    void addMember_UserNotFound_ThrowsException() {
        // Arrange
        Long roomId = 1L;
        Long userId = 999L;

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chatRoomService.addMember(roomId, userId, MemberRole.MEMBER));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void addMember_AlreadyMember_ReturnsExistingMembership() {
        // Arrange
        Long roomId = 1L;
        Long userId = 1L;

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.of(testMembership));

        // Act
        RoomMembership result = chatRoomService.addMember(roomId, userId, MemberRole.MEMBER);

        // Assert
        assertNotNull(result);
        assertEquals(testMembership, result);
        verify(roomMembershipRepository, never()).save(any(RoomMembership.class));
    }

    @Test
    void removeMember_ValidInput_DeletesMembership() {
        // Arrange
        Long roomId = 1L;
        Long userId = 1L;

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.of(testMembership));

        // Act
        chatRoomService.removeMember(roomId, userId);

        // Assert
        verify(roomMembershipRepository).deleteByUserAndChatRoom(testUser, testRoom);
    }

    @Test
    void removeMember_UserNotFound_ThrowsException() {
        // Arrange
        Long roomId = 1L;
        Long userId = 999L;

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chatRoomService.removeMember(roomId, userId));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void removeMember_NotAMember_ThrowsException() {
        // Arrange
        Long roomId = 1L;
        Long userId = 2L;
        User nonMember = new User();
        nonMember.setId(userId);

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(userId)).thenReturn(Optional.of(nonMember));
        when(roomMembershipRepository.findByUserAndChatRoom(nonMember, testRoom))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chatRoomService.removeMember(roomId, userId));
        assertEquals("User is not a member of this room", exception.getMessage());
    }

    @Test
    void deleteRoom_AsOwner_DeletesRoom() {
        // Arrange
        Long roomId = 1L;
        Long userId = 1L;

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.of(testMembership));

        // Act
        chatRoomService.deleteRoom(roomId, userId);

        // Assert
        verify(chatRoomRepository).delete(testRoom);
    }

    @Test
    void deleteRoom_AsModerator_DeletesRoom() {
        // Arrange
        Long roomId = 1L;
        Long userId = 1L;
        testMembership.setRole(MemberRole.MODERATOR);

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.of(testMembership));

        // Act
        chatRoomService.deleteRoom(roomId, userId);

        // Assert
        verify(chatRoomRepository).delete(testRoom);
    }

    @Test
    void deleteRoom_AsMember_ThrowsUnauthorizedException() {
        // Arrange
        Long roomId = 1L;
        Long userId = 1L;
        testMembership.setRole(MemberRole.MEMBER);

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.of(testMembership));

        // Act & Assert
        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> chatRoomService.deleteRoom(roomId, userId));
        assertEquals("Only owners or moderators can delete rooms", exception.getMessage());
        verify(chatRoomRepository, never()).delete(any(ChatRoom.class));
    }

    @Test
    void deleteRoom_UserNotFound_ThrowsUserNotFoundException() {
        // Arrange
        Long roomId = 1L;
        Long userId = 999L;

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                UserNotFoundException.class,
                () -> chatRoomService.deleteRoom(roomId, userId));
        verify(chatRoomRepository, never()).delete(any(ChatRoom.class));
    }

    @Test
    void deleteRoom_RoomNotFound_ThrowsRoomNotFoundException() {
        // Arrange
        Long roomId = 999L;
        Long userId = 1L;

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                RoomNotFoundException.class,
                () -> chatRoomService.deleteRoom(roomId, userId));
        verify(chatRoomRepository, never()).delete(any(ChatRoom.class));
    }

    @Test
    void deleteRoom_UserNotMember_ThrowsUnauthorizedException() {
        // Arrange
        Long roomId = 1L;
        Long userId = 1L;

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
                .thenReturn(Optional.empty());

        // Act & Assert
        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> chatRoomService.deleteRoom(roomId, userId));
        assertEquals("User is not a member of this room", exception.getMessage());
        verify(chatRoomRepository, never()).delete(any(ChatRoom.class));
    }
}
