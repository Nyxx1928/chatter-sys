package org.example.chat.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.chat.dto.CreateRoomRequest;
import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.User;
import org.example.chat.repository.ChatRoomRepository;
import org.example.chat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fix checking test for CSRF token validation on state-changing requests.
 * Verifies that state-changing requests without CSRF token are rejected.
 *
 * Validates: Requirements 2.9, 2.10, 2.11, 2.12
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CsrfTokenValidationFixIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setDisplayName("Test User");
        testUser.setPasswordHash("hashedpassword");
        testUser.setOnline(true);
        testUser = userRepository.save(testUser);
    }

    /**
     * Test 3.3: Write fix checking test for CSRF token validation on state-changing requests
     *
     * Verifies that state-changing requests without CSRF token are rejected.
     *
     * Acceptance Criteria:
     * - Test sends POST request to /api/rooms WITHOUT CSRF token
     * - Test verifies HTTP 403 Forbidden response
     * - Test verifies room is NOT created
     * - Test verifies error message is returned
     * - Test uses Spring Boot test framework with MockMvc
     * - Test passes with fixed code
     */
    @Test
    @WithMockUser(username = "testuser")
    void testPostRequestWithoutCsrfTokenIsRejected() throws Exception {
        // Arrange
        CreateRoomRequest request = new CreateRoomRequest();
        request.setName("New Room");
        request.setDescription("A new room");

        long initialRoomCount = chatRoomRepository.count();

        // Act & Assert
        mockMvc.perform(post("/api/rooms")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request))
                // Explicitly NOT including X-CSRF-TOKEN header
        )
        .andExpect(status().isForbidden());

        // Assert room is NOT created
        long finalRoomCount = chatRoomRepository.count();
        assertEquals(initialRoomCount, finalRoomCount,
            "Room should not be created without CSRF token");

        // Verify no room with this name exists
        ChatRoom createdRoom = chatRoomRepository.findAll().stream()
            .filter(r -> "New Room".equals(r.getName()))
            .findFirst()
            .orElse(null);
        assertNull(createdRoom, "Room should not be created without CSRF token");
    }

    /**
     * Test that POST request with invalid CSRF token is rejected
     */
    @Test
    @WithMockUser(username = "testuser")
    void testPostRequestWithInvalidCsrfTokenIsRejected() throws Exception {
        // Arrange
        CreateRoomRequest request = new CreateRoomRequest();
        request.setName("New Room");
        request.setDescription("A new room");

        long initialRoomCount = chatRoomRepository.count();

        // Act & Assert
        mockMvc.perform(post("/api/rooms")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request))
                .header("X-CSRF-TOKEN", "invalid-token-12345")
        )
        .andExpect(status().isForbidden());

        // Assert room is NOT created
        long finalRoomCount = chatRoomRepository.count();
        assertEquals(initialRoomCount, finalRoomCount,
            "Room should not be created with invalid CSRF token");
    }

    /**
     * Test that PUT request without CSRF token is rejected
     */
    @Test
    @WithMockUser(username = "testuser")
    void testPutRequestWithoutCsrfTokenIsRejected() throws Exception {
        // Arrange
        // Create a room first
        ChatRoom room = new ChatRoom();
        room.setName("Test Room");
        room.setDescription("Original description");
        room.setCreatedBy(testUser);
        room = chatRoomRepository.save(room);

        CreateRoomRequest updateRequest = new CreateRoomRequest();
        updateRequest.setName("Updated Room");
        updateRequest.setDescription("Updated description");

        // Act & Assert
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/rooms/" + room.getId())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(updateRequest))
                // Explicitly NOT including X-CSRF-TOKEN header
        )
        .andExpect(status().isForbidden());

        // Assert room is NOT updated
        ChatRoom unchangedRoom = chatRoomRepository.findById(room.getId()).orElse(null);
        assertNotNull(unchangedRoom, "Room should still exist");
        assertEquals("Test Room", unchangedRoom.getName(),
            "Room name should not be updated without CSRF token");
        assertEquals("Original description", unchangedRoom.getDescription(),
            "Room description should not be updated without CSRF token");
    }

    /**
     * Test that DELETE request without CSRF token is rejected
     */
    @Test
    @WithMockUser(username = "testuser")
    void testDeleteRequestWithoutCsrfTokenIsRejected() throws Exception {
        // Arrange
        // Create a room first
        ChatRoom room = new ChatRoom();
        room.setName("Test Room");
        room.setDescription("A test room");
        room.setCreatedBy(testUser);
        room = chatRoomRepository.save(room);

        long initialRoomCount = chatRoomRepository.count();

        // Act & Assert
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/rooms/" + room.getId())
                // Explicitly NOT including X-CSRF-TOKEN header
        )
        .andExpect(status().isForbidden());

        // Assert room is NOT deleted
        long finalRoomCount = chatRoomRepository.count();
        assertEquals(initialRoomCount, finalRoomCount,
            "Room should not be deleted without CSRF token");

        // Verify room still exists
        ChatRoom stillExistingRoom = chatRoomRepository.findById(room.getId()).orElse(null);
        assertNotNull(stillExistingRoom, "Room should still exist after DELETE without CSRF token");
    }
}
