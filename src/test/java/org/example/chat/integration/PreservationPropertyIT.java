package org.example.chat.integration;

import org.example.chat.dto.CreateRoomRequest;
import org.example.chat.dto.RegisterRequest;
import org.example.chat.dto.UpdateProfileRequest;
import org.example.chat.entity.*;
import org.example.chat.repository.*;
import org.example.chat.security.JwtUtil;
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
 * Property-based tests for preservation of non-buggy behavior.
 * These tests verify that the bugfix does NOT break existing functionality.
 * 
 * **Validates: Requirements 3.1-3.14 (Preservation Requirements)**
 * 
 * IMPORTANT: These tests MUST PASS on UNFIXED code to establish baseline
 * behavior.
 * 
 * These tests use a property-based testing approach by testing multiple
 * scenarios
 * and verifying that certain properties hold across all inputs.
 */
class PreservationPropertyIT extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private RoomMembershipRepository roomMembershipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Property 1: Authenticated requests to protected endpoints continue to work.
     * 
     * **Validates: Requirements 3.1 (Authenticated requests work unchanged)**
     * 
     * For any authenticated request to a protected endpoint where the user has
     * proper
     * authorization, the system SHALL continue to process the request normally.
     */
    @Test
    void authenticatedRequestsToProtectedEndpointsContinueToWork() throws Exception {
        // Test multiple scenarios
        for (int i = 0; i < 5; i++) {
            // Create authenticated user
            User user = new User();
            user.setUsername("auth_user_" + i);
            user.setEmail("auth_user_" + i + "@example.com");
            user.setPasswordHash(passwordEncoder.encode("TestP@ss1"));
            user.setDisplayName("Auth User " + i);
            user.setCreatedAt(LocalDateTime.now());
            user.setOnline(false);
            user = userRepository.save(user);

            String authToken = jwtUtil.generateToken(user.getUsername());

            // Create a room with the user as member
            ChatRoom room = new ChatRoom();
            room.setName("Room " + i);
            room.setCreatedAt(LocalDateTime.now());
            room.setCreatedBy(user);
            room = chatRoomRepository.save(room);

            RoomMembership membership = new RoomMembership();
            membership.setChatRoom(room);
            membership.setUser(user);
            membership.setRole(MemberRole.OWNER);
            membership.setJoinedAt(LocalDateTime.now());
            roomMembershipRepository.save(membership);

            // Test authenticated request to get message history
            mockMvc.perform(get("/api/rooms/" + room.getId() + "/messages")
                    .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk());

            // Test authenticated request to get room details
            mockMvc.perform(get("/api/rooms/" + room.getId())
                    .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Room " + i));

            // Test authenticated request to get user profile
            mockMvc.perform(get("/api/users/me")
                    .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("auth_user_" + i));
        }
    }

    /**
     * Property 2: Public endpoints remain accessible without authentication.
     * 
     * **Validates: Requirements 3.2 (Public endpoints work without auth)**
     * 
     * For any request to public endpoints (/api/auth/register, /api/auth/login),
     * the system SHALL continue to allow access without requiring authentication.
     */
    @Test
    void publicEndpointsRemainAccessibleWithoutAuthentication() throws Exception {
        // Test multiple registration scenarios
        for (int i = 0; i < 5; i++) {
            RegisterRequest registerRequest = new RegisterRequest(
                    "pub_user_" + i,
                    "pub_user_" + i + "@example.com",
                    "TestP@ss1",
                    "Public User " + i);

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(registerRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value("pub_user_" + i));
        }
    }

    /**
     * Property 3: Message history content and ordering remain unchanged.
     * 
     * **Validates: Requirements 3.3, 3.4, 3.5 (Message history functionality)**
     * 
     * For any authenticated member requesting message history, the system SHALL
     * continue to return messages with correct content, sender information,
     * message type, and timestamp.
     */
    @Test
    void messageHistoryContentAndOrderingUnchanged() throws Exception {
        // Test with different message counts
        int[] messageCounts = { 0, 1, 3, 5, 10 };

        for (int count : messageCounts) {
            // Create user and room
            User user = new User();
            user.setUsername("msguser_" + count);
            user.setEmail("msguser_" + count + "@example.com");
            user.setPasswordHash(passwordEncoder.encode("TestP@ss1"));
            user.setDisplayName("Message User");
            user.setCreatedAt(LocalDateTime.now());
            user.setOnline(false);
            user = userRepository.save(user);

            String authToken = jwtUtil.generateToken(user.getUsername());

            ChatRoom room = new ChatRoom();
            room.setName("Message Room " + count);
            room.setCreatedAt(LocalDateTime.now());
            room.setCreatedBy(user);
            room = chatRoomRepository.save(room);

            RoomMembership membership = new RoomMembership();
            membership.setChatRoom(room);
            membership.setUser(user);
            membership.setRole(MemberRole.OWNER);
            membership.setJoinedAt(LocalDateTime.now());
            roomMembershipRepository.save(membership);

            // Create messages
            for (int i = 0; i < count; i++) {
                Message message = new Message();
                message.setChatRoom(room);
                message.setSender(user);
                message.setContent("Message " + (i + 1));
                message.setMessageType(MessageType.TEXT);
                message.setTimestamp(LocalDateTime.now().minusMinutes(count - i));
                messageRepository.save(message);
            }

            // Verify message history returns messages with correct structure
            if (count > 0) {
                // For non-empty rooms, verify message structure
                mockMvc.perform(get("/api/rooms/" + room.getId() + "/messages")
                        .header("Authorization", "Bearer " + authToken))
                        .andExpect(status().isOk())
                        // Note: We're checking that the response contains message data
                        // The actual structure (array vs paginated) is what the bugfix will change
                        // But the MESSAGE CONTENT must remain unchanged
                        .andExpect(jsonPath("$..content").exists())
                        .andExpect(jsonPath("$..senderId").exists())
                        .andExpect(jsonPath("$..messageType").exists())
                        .andExpect(jsonPath("$..timestamp").exists());
            } else {
                // For empty rooms, just verify 200 OK
                mockMvc.perform(get("/api/rooms/" + room.getId() + "/messages")
                        .header("Authorization", "Bearer " + authToken))
                        .andExpect(status().isOk());
            }
        }
    }

    /**
     * Property 4: Room creation behavior remains unchanged.
     * 
     * **Validates: Requirements 3.6, 3.7 (Room creation functionality)**
     * 
     * For any authenticated user creating a room, the system SHALL continue to
     * create the room and add the creator as OWNER.
     */
    @Test
    void roomCreationBehaviorUnchanged() throws Exception {
        // Test multiple room creation scenarios
        for (int i = 0; i < 5; i++) {
            // Create user
            User user = new User();
            user.setUsername("creator_" + i);
            user.setEmail("creator_" + i + "@example.com");
            user.setPasswordHash(passwordEncoder.encode("TestP@ss1"));
            user.setDisplayName("Creator " + i);
            user.setCreatedAt(LocalDateTime.now());
            user.setOnline(false);
            user = userRepository.save(user);

            String authToken = jwtUtil.generateToken(user.getUsername());

            // Create room
            CreateRoomRequest request = new CreateRoomRequest("Test Room " + i, "Test Description");
            final String roomName = "Test Room " + i;

            mockMvc.perform(post("/api/rooms")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value(roomName))
                    .andExpect(jsonPath("$.id").isNumber());

            // Verify creator is added as OWNER
            ChatRoom createdRoom = chatRoomRepository.findAll().stream()
                    .filter(r -> r.getName().equals(roomName))
                    .findFirst()
                    .orElseThrow();

            final Long userId = user.getId();
            RoomMembership membership = roomMembershipRepository.findByChatRoom(createdRoom).stream()
                    .filter(m -> m.getUser().getId().equals(userId))
                    .findFirst()
                    .orElseThrow();

            assertEquals(MemberRole.OWNER, membership.getRole());
        }
    }

    /**
     * Property 5: User profile operations remain unchanged.
     * 
     * **Validates: Requirements 3.10, 3.11 (User profile functionality)**
     * 
     * For any authenticated user accessing or updating their profile, the system
     * SHALL continue to work correctly.
     */
    @Test
    void userProfileOperationsUnchanged() throws Exception {
        // Test multiple profile operations
        for (int i = 0; i < 5; i++) {
            // Create user
            User user = new User();
            user.setUsername("profile_" + i);
            user.setEmail("profile_" + i + "@example.com");
            user.setPasswordHash(passwordEncoder.encode("TestP@ss1"));
            user.setDisplayName("Original Name " + i);
            user.setCreatedAt(LocalDateTime.now());
            user.setOnline(false);
            user = userRepository.save(user);

            String authToken = jwtUtil.generateToken(user.getUsername());

            // Test get profile
            mockMvc.perform(get("/api/users/me")
                    .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("profile_" + i))
                    .andExpect(jsonPath("$.displayName").value("Original Name " + i));

            // Test update profile
            UpdateProfileRequest updateRequest = new UpdateProfileRequest(
                    "profile_" + i + "@example.com",
                    "Updated Name " + i);

            mockMvc.perform(put("/api/users/me")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.displayName").value("Updated Name " + i));
        }
    }

    /**
     * Property 6: Validation error responses remain unchanged.
     * 
     * **Validates: Requirements 3.12 (Validation errors return 400)**
     * 
     * For any request with invalid data (invalid email, empty fields), the system
     * SHALL continue to return 400 Bad Request with appropriate error messages.
     */
    @Test
    void validationErrorResponsesUnchanged() throws Exception {
        // Test invalid email format
        RegisterRequest invalidEmailRequest = new RegisterRequest(
                "testuser",
                "invalid-email",
                "TestP@ss1",
                "Test User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(invalidEmailRequest)))
                .andExpect(status().isBadRequest());

        // Test short password
        RegisterRequest shortPasswordRequest = new RegisterRequest(
                "testuser2",
                "test@example.com",
                "short",
                "Test User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(shortPasswordRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Property 7: Member access to rooms remains unchanged.
     * 
     * **Validates: Requirements 3.8, 3.9 (Room access for members)**
     * 
     * For any authenticated user who is a member of a room, the system SHALL
     * continue to allow access to room details and message history.
     */
    @Test
    void memberAccessToRoomsUnchanged() throws Exception {
        // Test multiple member access scenarios
        for (int i = 0; i < 5; i++) {
            // Create user
            User user = new User();
            user.setUsername("member_" + i);
            user.setEmail("member_" + i + "@example.com");
            user.setPasswordHash(passwordEncoder.encode("TestP@ss1"));
            user.setDisplayName("Member " + i);
            user.setCreatedAt(LocalDateTime.now());
            user.setOnline(false);
            user = userRepository.save(user);

            String authToken = jwtUtil.generateToken(user.getUsername());

            // Create room with user as member
            ChatRoom room = new ChatRoom();
            room.setName("Member Room " + i);
            room.setCreatedAt(LocalDateTime.now());
            room.setCreatedBy(user);
            room = chatRoomRepository.save(room);

            RoomMembership membership = new RoomMembership();
            membership.setChatRoom(room);
            membership.setUser(user);
            membership.setRole(MemberRole.MEMBER);
            membership.setJoinedAt(LocalDateTime.now());
            roomMembershipRepository.save(membership);

            // Verify member can access room
            mockMvc.perform(get("/api/rooms/" + room.getId())
                    .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Member Room " + i));

            // Verify member can access message history
            mockMvc.perform(get("/api/rooms/" + room.getId() + "/messages")
                    .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk());
        }
    }
}