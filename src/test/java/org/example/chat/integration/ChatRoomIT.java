package org.example.chat.integration;

import org.example.chat.dto.CreateRoomRequest;
import org.example.chat.dto.RegisterRequest;
import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.MemberRole;
import org.example.chat.entity.RoomMembership;
import org.example.chat.entity.User;
import org.example.chat.repository.ChatRoomRepository;
import org.example.chat.repository.RoomMembershipRepository;
import org.example.chat.repository.UserRepository;
import org.example.chat.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for chat room operations.
 * Tests room creation, membership, and retrieval with real database.
 */
class ChatRoomIT extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private RoomMembershipRepository roomMembershipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private User testUser;
    private String authToken;

    @BeforeEach
    void setUp() {
        // Create a test user
        testUser = new User();
        testUser.setUsername("roomtestuser");
        testUser.setEmail("roomtest@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("password123"));
        testUser.setDisplayName("Room Test User");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setOnline(false);
        testUser = userRepository.save(testUser);

        // Generate auth token
        authToken = jwtUtil.generateToken(testUser.getUsername());
    }

    @Test
    void createRoom_ValidRequest_Success() throws Exception {
        CreateRoomRequest request = new CreateRoomRequest("Test Room", "Test Description");

        mockMvc.perform(post("/api/rooms")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Room"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        // Verify room was created in database
        List<ChatRoom> rooms = chatRoomRepository.findAll();
        assertEquals(1, rooms.size());
        assertEquals("Test Room", rooms.get(0).getName());

        // Verify creator was added as OWNER
        List<RoomMembership> memberships = roomMembershipRepository.findByChatRoom(rooms.get(0));
        assertEquals(1, memberships.size());
        assertEquals(testUser.getId(), memberships.get(0).getUser().getId());
        assertEquals(MemberRole.OWNER, memberships.get(0).getRole());
    }

    @Test
    void createRoom_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        CreateRoomRequest request = new CreateRoomRequest("Test Room", "Test Description");

        mockMvc.perform(post("/api/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isUnauthorized());

        // Verify no room was created
        assertEquals(0, chatRoomRepository.count());
    }

    @Test
    void getRooms_UserHasMultipleRooms_ReturnsAllRooms() throws Exception {
        // Create multiple rooms
        ChatRoom room1 = new ChatRoom();
        room1.setName("Room 1");
        room1.setCreatedAt(LocalDateTime.now());
        room1 = chatRoomRepository.save(room1);

        ChatRoom room2 = new ChatRoom();
        room2.setName("Room 2");
        room2.setCreatedAt(LocalDateTime.now());
        room2 = chatRoomRepository.save(room2);

        // Add user as member to both rooms
        RoomMembership membership1 = new RoomMembership();
        membership1.setChatRoom(room1);
        membership1.setUser(testUser);
        membership1.setRole(MemberRole.OWNER);
        membership1.setJoinedAt(LocalDateTime.now());
        roomMembershipRepository.save(membership1);

        RoomMembership membership2 = new RoomMembership();
        membership2.setChatRoom(room2);
        membership2.setUser(testUser);
        membership2.setRole(MemberRole.MEMBER);
        membership2.setJoinedAt(LocalDateTime.now());
        roomMembershipRepository.save(membership2);

        // Get rooms for user
        mockMvc.perform(get("/api/rooms")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Room 1", "Room 2")));
    }

    @Test
    void getRooms_UserHasNoRooms_ReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/rooms")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getRoomById_ExistingRoom_ReturnsRoom() throws Exception {
        // Create a room
        ChatRoom room = new ChatRoom();
        room.setName("Test Room");
        room.setCreatedAt(LocalDateTime.now());
        room = chatRoomRepository.save(room);

        // Add user as member
        RoomMembership membership = new RoomMembership();
        membership.setChatRoom(room);
        membership.setUser(testUser);
        membership.setRole(MemberRole.OWNER);
        membership.setJoinedAt(LocalDateTime.now());
        roomMembershipRepository.save(membership);

        // Get room by ID
        mockMvc.perform(get("/api/rooms/" + room.getId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(room.getId()))
                .andExpect(jsonPath("$.name").value("Test Room"));
    }

    @Test
    void getRoomById_NonexistentRoom_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/rooms/99999")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRoomById_UserNotMember_AutoJoinsAndReturnsRoom() throws Exception {
        // Create a room without adding the test user as member
        ChatRoom room = new ChatRoom();
        room.setName("Private Room");
        room.setCreatedAt(LocalDateTime.now());
        room = chatRoomRepository.save(room);

        // Create another user and add them as owner
        User otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");
        otherUser.setPasswordHash(passwordEncoder.encode("password123"));
        otherUser.setDisplayName("Other User");
        otherUser.setCreatedAt(LocalDateTime.now());
        otherUser.setOnline(false);
        otherUser = userRepository.save(otherUser);

        RoomMembership membership = new RoomMembership();
        membership.setChatRoom(room);
        membership.setUser(otherUser);
        membership.setRole(MemberRole.OWNER);
        membership.setJoinedAt(LocalDateTime.now());
        roomMembershipRepository.save(membership);

        // Access room as non-member (auto-joins)
        mockMvc.perform(get("/api/rooms/" + room.getId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(room.getId()))
                .andExpect(jsonPath("$.name").value("Private Room"));

        assertTrue(roomMembershipRepository.findByUserAndChatRoom(testUser, room).isPresent());
    }

    @Test
    void completeRoomFlow_CreateAndRetrieve_Success() throws Exception {
        // Step 1: Create a room
        CreateRoomRequest createRequest = new CreateRoomRequest("Integration Test Room", "Test Description");

        String response = mockMvc.perform(post("/api/rooms")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long roomId = objectMapper.readTree(response).get("id").asLong();

        // Step 2: Retrieve the room
        mockMvc.perform(get("/api/rooms/" + roomId)
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Integration Test Room"));

        // Step 3: Verify room appears in user's room list
        mockMvc.perform(get("/api/rooms")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Integration Test Room"));
    }
}