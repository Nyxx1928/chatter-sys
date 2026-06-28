package org.example.chat.integration;

import org.example.chat.dto.*;
import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.User;
import org.example.chat.repository.ChatRoomRepository;
import org.example.chat.repository.FriendRequestRepository;
import org.example.chat.repository.FriendshipRepository;
import org.example.chat.repository.UserRepository;
import org.example.chat.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for social discovery and room management.
 * Tests complete user flows: search, friend requests, room creation, and room deletion.
 * 
 * Requirements tested:
 * - 1.1: User search returns matching users
 * - 2.1: Friend request creation and duplicate prevention
 * - 3.1: Friends list display
 * - 4.1: Room creation with valid data
 * - 6.1: Room deletion by authorized users
 */
class SocialDiscoveryAndRoomManagementIT extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private User user1;
    private User user2;
    private String user1Token;
    private String user2Token;

    @BeforeEach
    void setUp() {
        // Create first test user
        user1 = new User();
        user1.setUsername("alice");
        user1.setEmail("alice@example.com");
        user1.setPasswordHash(passwordEncoder.encode("TestP@ss1"));
        user1.setDisplayName("Alice Smith");
        user1.setCreatedAt(LocalDateTime.now());
        user1.setOnline(false);
        user1 = userRepository.save(user1);
        user1Token = jwtUtil.generateToken(user1.getUsername());

        // Create second test user
        user2 = new User();
        user2.setUsername("bob");
        user2.setEmail("bob@example.com");
        user2.setPasswordHash(passwordEncoder.encode("TestP@ss1"));
        user2.setDisplayName("Bob Johnson");
        user2.setCreatedAt(LocalDateTime.now());
        user2.setOnline(false);
        user2 = userRepository.save(user2);
        user2Token = jwtUtil.generateToken(user2.getUsername());
    }

    /**
     * Complete end-to-end flow: search user, send request, accept, create room, delete room.
     * Validates Requirements: 1.1, 2.1, 3.1, 4.1, 6.1
     */
    @Test
    void completeFlow_SearchSendAcceptCreateDelete_Success() throws Exception {
        // Step 1: User1 searches for User2 (Requirement 1.1)
        mockMvc.perform(get("/api/users/search")
                .param("q", "bob")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].user.username").value("bob"))
                .andExpect(jsonPath("$[0].user.displayName").value("Bob Johnson"))
                .andExpect(jsonPath("$[0].relationshipStatus").value("NONE"));

        // Step 2: User1 sends friend request to User2 (Requirement 2.1)
        FriendRequestCreateRequest friendRequest = new FriendRequestCreateRequest(user2.getId());
        
        String requestResponse = mockMvc.perform(post("/api/friends/requests")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(friendRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.requester.username").value("alice"))
                .andExpect(jsonPath("$.recipient.username").value("bob"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long requestId = objectMapper.readTree(requestResponse).get("id").asLong();

        // Verify duplicate prevention (Requirement 2.1)
        mockMvc.perform(post("/api/friends/requests")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(friendRequest)))
                .andExpect(status().isConflict());

        // Step 3: User2 views pending requests
        mockMvc.perform(get("/api/friends/requests")
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incoming", hasSize(1)))
                .andExpect(jsonPath("$.incoming[0].requester.username").value("alice"))
                .andExpect(jsonPath("$.outgoing", hasSize(0)));

        // Step 4: User2 accepts the friend request (Requirement 2.1)
        mockMvc.perform(post("/api/friends/requests/" + requestId + "/accept")
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friend.username").value("alice"));

        // Verify friendship was created (bidirectional, so at least 1 record)
        assertTrue(friendshipRepository.count() >= 1);

        // Step 5: Both users can see each other in friends list (Requirement 3.1)
        mockMvc.perform(get("/api/friends")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("bob"));

        mockMvc.perform(get("/api/friends")
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("alice"));

        // Step 6: User1 creates a chat room (Requirement 4.1)
        CreateRoomRequest createRoomRequest = new CreateRoomRequest("Test Room", "A test room");
        
        String roomResponse = mockMvc.perform(post("/api/rooms")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(createRoomRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Room"))
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long roomId = objectMapper.readTree(roomResponse).get("id").asLong();

        // Verify room appears in list
        mockMvc.perform(get("/api/rooms")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[?(@.name == 'Test Room')]").exists());

        // Step 7: User1 deletes the room (Requirement 6.1)
        mockMvc.perform(delete("/api/rooms/" + roomId)
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNoContent());

        // Verify room was deleted
        assertFalse(chatRoomRepository.findById(roomId).isPresent());
    }

    /**
     * Tests user search with various queries.
     * Validates Requirement 1.1
     */
    @Test
    void userSearch_VariousQueries_ReturnsMatchingUsers() throws Exception {
        // Search by username
        mockMvc.perform(get("/api/users/search")
                .param("q", "alice")
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].user.username").value("alice"));

        // Search by display name
        mockMvc.perform(get("/api/users/search")
                .param("q", "Smith")
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].user.displayName").value("Alice Smith"));

        // Case-insensitive search
        mockMvc.perform(get("/api/users/search")
                .param("q", "BOB")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].user.username").value("bob"));

        // Empty query returns empty results
        mockMvc.perform(get("/api/users/search")
                .param("q", "")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // No matches
        mockMvc.perform(get("/api/users/search")
                .param("q", "nonexistent")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    /**
     * Tests user search shows relationship status correctly.
     * Validates Requirement 1.1 (relationship status indication)
     */
    @Test
    void userSearch_WithRelationshipStatus_ShowsCorrectStatus() throws Exception {
        // Initially no relationship
        mockMvc.perform(get("/api/users/search")
                .param("q", "bob")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].relationshipStatus").value("NONE"));

        // Send friend request
        FriendRequestCreateRequest request = new FriendRequestCreateRequest(user2.getId());
        mockMvc.perform(post("/api/friends/requests")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isCreated());

        // User1 sees PENDING_OUTGOING
        mockMvc.perform(get("/api/users/search")
                .param("q", "bob")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].relationshipStatus").value("PENDING_OUTGOING"));

        // User2 sees PENDING_INCOMING
        mockMvc.perform(get("/api/users/search")
                .param("q", "alice")
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].relationshipStatus").value("PENDING_INCOMING"));
    }

    /**
     * Tests friend request lifecycle: send, accept, decline.
     * Validates Requirement 2.1
     */
    @Test
    void friendRequest_SendAcceptDecline_Success() throws Exception {
        // Send request from user1 to user2
        FriendRequestCreateRequest request = new FriendRequestCreateRequest(user2.getId());
        
        String response = mockMvc.perform(post("/api/friends/requests")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long requestId = objectMapper.readTree(response).get("id").asLong();

        // User2 can see incoming request
        mockMvc.perform(get("/api/friends/requests")
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incoming", hasSize(1)))
                .andExpect(jsonPath("$.incoming[0].id").value(requestId));

        // User2 accepts
        mockMvc.perform(post("/api/friends/requests/" + requestId + "/accept")
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk());

        // Request is removed from pending
        mockMvc.perform(get("/api/friends/requests")
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incoming", hasSize(0)));

        // Friendship exists
        mockMvc.perform(get("/api/friends")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    /**
     * Tests declining a friend request.
     * Validates Requirement 2.1
     */
    @Test
    void friendRequest_Decline_RemovesRequest() throws Exception {
        // Send request
        FriendRequestCreateRequest request = new FriendRequestCreateRequest(user2.getId());
        
        String response = mockMvc.perform(post("/api/friends/requests")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long requestId = objectMapper.readTree(response).get("id").asLong();

        // User2 declines
        mockMvc.perform(post("/api/friends/requests/" + requestId + "/decline")
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isNoContent());

        // Request is removed
        mockMvc.perform(get("/api/friends/requests")
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incoming", hasSize(0)));

        // No friendship created
        mockMvc.perform(get("/api/friends")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    /**
     * Tests self-request validation.
     * Validates Requirement 2.1 (self-request rejection)
     */
    @Test
    void friendRequest_ToSelf_ReturnsBadRequest() throws Exception {
        FriendRequestCreateRequest request = new FriendRequestCreateRequest(user1.getId());
        
        mockMvc.perform(post("/api/friends/requests")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Tests friends list display.
     * Validates Requirement 3.1
     */
    @Test
    void friendsList_AfterAcceptance_ShowsFriends() throws Exception {
        // Create friendship
        FriendRequestCreateRequest request = new FriendRequestCreateRequest(user2.getId());
        String response = mockMvc.perform(post("/api/friends/requests")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long requestId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(post("/api/friends/requests/" + requestId + "/accept")
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk());

        // Both users see each other in friends list
        mockMvc.perform(get("/api/friends")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("bob"))
                .andExpect(jsonPath("$[0].displayName").value("Bob Johnson"));

        mockMvc.perform(get("/api/friends")
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].displayName").value("Alice Smith"));
    }

    /**
     * Tests empty friends list.
     * Validates Requirement 3.1 (empty state)
     */
    @Test
    void friendsList_NoFriends_ReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/friends")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    /**
     * Tests room creation and navigation.
     * Validates Requirement 4.1
     */
    @Test
    void createRoom_ValidData_Success() throws Exception {
        CreateRoomRequest request = new CreateRoomRequest("My Room", "Room description");
        
        String response = mockMvc.perform(post("/api/rooms")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("My Room"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long roomId = objectMapper.readTree(response).get("id").asLong();

        // Room appears in list
        mockMvc.perform(get("/api/rooms")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + roomId + ")].name").value("My Room"));

        // Can retrieve room by ID
        mockMvc.perform(get("/api/rooms/" + roomId)
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("My Room"));
    }

    /**
     * Tests room creation without authentication.
     * Validates Requirement 4.1 (authentication required)
     */
    @Test
    void createRoom_WithoutAuth_ReturnsUnauthorized() throws Exception {
        CreateRoomRequest request = new CreateRoomRequest("Test Room", "Description");
        
        mockMvc.perform(post("/api/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Tests room deletion by owner.
     * Validates Requirement 6.1
     */
    @Test
    void deleteRoom_ByOwner_Success() throws Exception {
        // Create room
        CreateRoomRequest request = new CreateRoomRequest("Room to Delete", "Will be deleted");
        
        String response = mockMvc.perform(post("/api/rooms")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long roomId = objectMapper.readTree(response).get("id").asLong();

        // Delete room
        mockMvc.perform(delete("/api/rooms/" + roomId)
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNoContent());

        // Verify room is deleted
        assertFalse(chatRoomRepository.findById(roomId).isPresent());
    }

    /**
     * Tests room deletion by non-owner.
     * Validates Requirement 6.1 (authorization check)
     */
    @Test
    void deleteRoom_ByNonOwner_ReturnsForbidden() throws Exception {
        // User1 creates room
        CreateRoomRequest request = new CreateRoomRequest("User1 Room", "Owned by user1");
        
        String response = mockMvc.perform(post("/api/rooms")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long roomId = objectMapper.readTree(response).get("id").asLong();

        // User2 tries to delete
        mockMvc.perform(delete("/api/rooms/" + roomId)
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());

        // Room still exists
        assertTrue(chatRoomRepository.findById(roomId).isPresent());
    }

    /**
     * Tests room deletion for nonexistent room.
     * Validates Requirement 6.1 (404 response)
     */
    @Test
    void deleteRoom_NonexistentRoom_ReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/rooms/99999")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNotFound());
    }

    /**
     * Tests authentication requirement for user search.
     * Validates Requirement 1.1 (authentication required)
     */
    @Test
    void userSearch_WithoutAuth_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/search")
                .param("q", "alice"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Tests authentication requirement for friend operations.
     * Validates Requirement 2.1 (authentication required)
     */
    @Test
    void friendOperations_WithoutAuth_ReturnsUnauthorized() throws Exception {
        FriendRequestCreateRequest request = new FriendRequestCreateRequest(user2.getId());
        
        // Send request without auth
        mockMvc.perform(post("/api/friends/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isUnauthorized());

        // List requests without auth
        mockMvc.perform(get("/api/friends/requests"))
                .andExpect(status().isUnauthorized());

        // List friends without auth
        mockMvc.perform(get("/api/friends"))
                .andExpect(status().isUnauthorized());
    }
}
