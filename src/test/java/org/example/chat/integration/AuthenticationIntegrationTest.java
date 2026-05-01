package org.example.chat.integration;

import org.example.chat.dto.LoginRequest;
import org.example.chat.dto.RegisterRequest;
import org.example.chat.entity.User;
import org.example.chat.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for authentication flow.
 * Tests the complete registration and login process with real database.
 */
class AuthenticationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void completeAuthenticationFlow_RegisterAndLogin_Success() throws Exception {
        // Step 1: Register a new user
        RegisterRequest registerRequest = new RegisterRequest(
            "integrationuser",
            "integration@example.com",
            "password123",
            "Integration Test User"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(registerRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("integrationuser"))
            .andExpect(jsonPath("$.email").value("integration@example.com"))
            .andExpect(jsonPath("$.displayName").value("Integration Test User"))
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.online").value(false));

        // Verify user was saved to database
        User savedUser = userRepository.findByUsername("integrationuser").orElse(null);
        assertNotNull(savedUser);
        assertEquals("integrationuser", savedUser.getUsername());
        assertEquals("integration@example.com", savedUser.getEmail());
        assertTrue(passwordEncoder.matches("password123", savedUser.getPasswordHash()));

        // Step 2: Login with the registered user
        LoginRequest loginRequest = new LoginRequest("integrationuser", "password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.user.username").value("integrationuser"))
            .andExpect(jsonPath("$.user.email").value("integration@example.com"))
            .andExpect(jsonPath("$.user.displayName").value("Integration Test User"));
    }

    @Test
    void register_DuplicateUsername_ReturnsBadRequest() throws Exception {
        // Create first user
        RegisterRequest firstRequest = new RegisterRequest(
            "duplicateuser",
            "first@example.com",
            "password123",
            "First User"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(firstRequest)))
            .andExpect(status().isCreated());

        // Try to create second user with same username
        RegisterRequest secondRequest = new RegisterRequest(
            "duplicateuser",
            "second@example.com",
            "password456",
            "Second User"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(secondRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Username already exists"));

        // Verify only one user exists
        assertEquals(1, userRepository.findAll().stream()
            .filter(u -> u.getUsername().equals("duplicateuser"))
            .count());
    }

    @Test
    void register_DuplicateEmail_ReturnsBadRequest() throws Exception {
        // Create first user
        RegisterRequest firstRequest = new RegisterRequest(
            "firstuser",
            "duplicate@example.com",
            "password123",
            "First User"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(firstRequest)))
            .andExpect(status().isCreated());

        // Try to create second user with same email
        RegisterRequest secondRequest = new RegisterRequest(
            "seconduser",
            "duplicate@example.com",
            "password456",
            "Second User"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(secondRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Email already exists"));

        // Verify only one user with this email exists
        assertEquals(1, userRepository.findAll().stream()
            .filter(u -> u.getEmail().equals("duplicate@example.com"))
            .count());
    }

    @Test
    void login_InvalidCredentials_ReturnsBadRequest() throws Exception {
        // Register a user
        RegisterRequest registerRequest = new RegisterRequest(
            "testuser",
            "test@example.com",
            "correctpassword",
            "Test User"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(registerRequest)))
            .andExpect(status().isCreated());

        // Try to login with wrong password
        LoginRequest loginRequest = new LoginRequest("testuser", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(loginRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void login_NonexistentUser_ReturnsBadRequest() throws Exception {
        LoginRequest loginRequest = new LoginRequest("nonexistent", "password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(loginRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void register_InvalidEmail_ReturnsBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "testuser",
            "invalid-email",
            "password123",
            "Test User"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
            .andExpect(status().isBadRequest());

        // Verify user was not created
        assertFalse(userRepository.findByUsername("testuser").isPresent());
    }

    @Test
    void register_ShortPassword_ReturnsBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "testuser",
            "test@example.com",
            "short",
            "Test User"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
            .andExpect(status().isBadRequest());

        // Verify user was not created
        assertFalse(userRepository.findByUsername("testuser").isPresent());
    }
}
