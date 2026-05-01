package org.example.chat.integration;

import org.example.chat.dto.UpdateProfileRequest;
import org.example.chat.entity.User;
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
 * Integration tests for user operations.
 * Tests user profile management with real database.
 */
class UserIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private User testUser;
    private String authToken;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUsername("profileuser");
        testUser.setEmail("profile@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("password123"));
        testUser.setDisplayName("Profile Test User");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setOnline(false);
        testUser = userRepository.save(testUser);

        // Generate auth token
        authToken = jwtUtil.generateToken(testUser.getUsername());
    }

    @Test
    void getCurrentUser_Authenticated_ReturnsUserProfile() throws Exception {
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + authToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("profileuser"))
            .andExpect(jsonPath("$.email").value("profile@example.com"))
            .andExpect(jsonPath("$.displayName").value("Profile Test User"))
            .andExpect(jsonPath("$.online").value(false));
    }

    @Test
    void getCurrentUser_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_ValidRequest_UpdatesUser() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest(
            "profile@example.com",
            "Updated Display Name"
        );

        mockMvc.perform(put("/api/users/me")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("Updated Display Name"))
            .andExpect(jsonPath("$.email").value("profile@example.com"))
            .andExpect(jsonPath("$.username").value("profileuser"));

        // Verify database was updated
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals("Updated Display Name", updatedUser.getDisplayName());
        assertEquals("profile@example.com", updatedUser.getEmail());
    }

    @Test
    void updateProfile_ChangeEmail_UpdatesEmail() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest(
            "newemail@example.com",
            "Profile Test User"
        );

        mockMvc.perform(put("/api/users/me")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("newemail@example.com"));

        // Verify database was updated
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals("newemail@example.com", updatedUser.getEmail());
    }

    @Test
    void updateProfile_InvalidEmail_ReturnsBadRequest() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest(
            "invalid-email",
            "Profile Test User"
        );

        mockMvc.perform(put("/api/users/me")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
            .andExpect(status().isBadRequest());

        // Verify database was not updated
        User unchangedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals("profile@example.com", unchangedUser.getEmail());
    }

    @Test
    void updateProfile_DuplicateEmail_ReturnsBadRequest() throws Exception {
        // Create another user
        User otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");
        otherUser.setPasswordHash(passwordEncoder.encode("password123"));
        otherUser.setDisplayName("Other User");
        otherUser.setCreatedAt(LocalDateTime.now());
        otherUser.setOnline(false);
        userRepository.save(otherUser);

        // Try to update to existing email
        UpdateProfileRequest request = new UpdateProfileRequest(
            "other@example.com",
            "Profile Test User"
        );

        mockMvc.perform(put("/api/users/me")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Email already exists"));

        // Verify database was not updated
        User unchangedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals("profile@example.com", unchangedUser.getEmail());
    }

    @Test
    void updateProfile_EmptyDisplayName_ReturnsBadRequest() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest(
            "profile@example.com",
            ""
        );

        mockMvc.perform(put("/api/users/me")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
            .andExpect(status().isBadRequest());

        // Verify database was not updated
        User unchangedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals("Profile Test User", unchangedUser.getDisplayName());
    }

    @Test
    void updateProfile_Unauthenticated_ReturnsUnauthorized() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest(
            "profile@example.com",
            "Updated Display Name"
        );

        mockMvc.perform(put("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void completeUserFlow_GetAndUpdate_Success() throws Exception {
        // Step 1: Get current profile
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + authToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("Profile Test User"));

        // Step 2: Update profile
        UpdateProfileRequest updateRequest = new UpdateProfileRequest(
            "updated@example.com",
            "Updated Name"
        );

        mockMvc.perform(put("/api/users/me")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("Updated Name"))
            .andExpect(jsonPath("$.email").value("updated@example.com"));

        // Step 3: Verify changes persisted
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + authToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("Updated Name"))
            .andExpect(jsonPath("$.email").value("updated@example.com"));
    }
}
