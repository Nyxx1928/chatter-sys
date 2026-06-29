package org.example.chat.integration;

import com.jayway.jsonpath.JsonPath;
import org.example.chat.dto.LoginRequest;
import org.example.chat.dto.RegisterRequest;
import org.example.chat.entity.PendingRegistration;
import org.example.chat.entity.User;
import org.example.chat.repository.PendingRegistrationRepository;
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
class AuthenticationIT extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void completeAuthenticationFlow_RegisterAndLogin_Success() throws Exception {
        // Step 1: Register a new user (creates pending registration)
        RegisterRequest registerRequest = new RegisterRequest(
                "integrationuser",
                "integration@example.com",
                "TestP@ss1",
                "Integration Test User");

        String registerResponse = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.emailSent").isBoolean())
                .andExpect(jsonPath("$.verificationUrl").exists())
                .andReturn().getResponse().getContentAsString();

        // Step 2: Verify email (extract verificationUrl from response)
        String verificationUrl = JsonPath.read(registerResponse, "$.verificationUrl");
        // verificationUrl is like "http://localhost:8080/api/auth/verify-email?token=..."
        String token = verificationUrl.substring(verificationUrl.indexOf("token=") + 6);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/auth/verify-email")
                .param("token", token))
                .andExpect(status().isFound()); // 302 redirect to frontend on success

        // Step 3: Login with the now-verified user
        LoginRequest loginRequest = new LoginRequest("integrationuser", "TestP@ss1");

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
                "TestP@ss1",
                "First User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(firstRequest)))
                .andExpect(status().isCreated());

        // Try to create second user with same username
        RegisterRequest secondRequest = new RegisterRequest(
                "duplicateuser",
                "second@example.com",
                "TestP@ss1",
                "Second User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(secondRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username already exists"));

        // Verify only one pending registration with this username exists
        assertEquals(1, pendingRegistrationRepository.findAll().stream()
                .filter(p -> p.getUsername().equals("duplicateuser"))
                .count());
    }

    @Test
    void register_DuplicateEmail_ReturnsBadRequest() throws Exception {
        // Create first user
        RegisterRequest firstRequest = new RegisterRequest(
                "firstuser",
                "duplicate@example.com",
                "TestP@ss1",
                "First User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(firstRequest)))
                .andExpect(status().isCreated());

        // Try to create second user with same email
        RegisterRequest secondRequest = new RegisterRequest(
                "seconduser",
                "duplicate@example.com",
                "TestP@ss1",
                "Second User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(secondRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already exists"));

        // Verify only one pending registration with this email exists
        assertEquals(1, pendingRegistrationRepository.findAll().stream()
                .filter(p -> p.getEmail().equals("duplicate@example.com"))
                .count());
    }

    @Test
    void login_InvalidCredentials_ReturnsBadRequest() throws Exception {
        // Register a user
        RegisterRequest registerRequest = new RegisterRequest(
                "testuser",
                "test@example.com",
                "TestP@ss1",
                "Test User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(registerRequest)))
                .andExpect(status().isCreated());

        // Try to login with wrong password
        LoginRequest loginRequest = new LoginRequest("testuser", "Wr0ngP@ss1");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void login_NonexistentUser_ReturnsBadRequest() throws Exception {
        LoginRequest loginRequest = new LoginRequest("nonexistent", "TestP@ss1");

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
                "TestP@ss1",
                "Test User");

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
                "Test User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isBadRequest());

        // Verify user was not created
        assertFalse(userRepository.findByUsername("testuser").isPresent());
    }
}