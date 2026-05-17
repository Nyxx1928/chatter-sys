package org.example.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.chat.config.WebMvcTestConfig;
import org.example.chat.dto.LoginRequest;
import org.example.chat.dto.RegisterRequest;
import org.example.chat.entity.User;
import org.example.chat.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AuthController.
 */
@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
@Import({WebMvcTestConfig.class, org.example.chat.exception.GlobalExceptionHandler.class}) // Import test config and exception handler
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for unit tests
@ActiveProfiles("test") // Use H2 in-memory database for tests
@TestPropertySource(properties = "app.verification.expose-link=true") // Enable verification URL in response for tests
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setDisplayName("Test User");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setOnline(false);
    }

    @Test
    void register_ValidRequest_ReturnsCreated() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest(
            "testuser",
            "test@example.com",
            "password123",
            "Test User"
        );

        when(authenticationService.registerUser(
            anyString(), anyString(), anyString(), anyString()
        )).thenReturn(new AuthenticationService.RegistrationResult(
                "token-123",
                "http://localhost:8080/api/auth/verify-email?token=token-123",
                true,
                null
        ));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.message").value("Registration initiated. Please check your email to verify your account."))
            .andExpect(jsonPath("$.emailSent").value(true))
            .andExpect(jsonPath("$.verificationUrl").exists())
            .andExpect(jsonPath("$.errorMessage").doesNotExist());
    }

    @Test
    void register_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Arrange - missing required fields
        RegisterRequest request = new RegisterRequest(
            "",  // Empty username
            "invalid-email",  // Invalid email
            "short",  // Too short password
            ""  // Empty display name
        );

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void register_DuplicateUsername_ReturnsBadRequest() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest(
            "testuser",
            "test@example.com",
            "password123",
            "Test User"
        );

        when(authenticationService.registerUser(
            anyString(), anyString(), anyString(), anyString()
        )).thenThrow(new IllegalArgumentException("Username already exists"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    void login_ValidCredentials_ReturnsToken() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "password123");
        String token = "jwt-token-here";

        when(authenticationService.authenticateUser(anyString(), anyString()))
            .thenReturn(token);
        when(authenticationService.getUserByUsername(anyString()))
            .thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value(token))
            .andExpect(jsonPath("$.user.username").value("testuser"))
            .andExpect(jsonPath("$.user.email").value("test@example.com"))
            .andExpect(jsonPath("$.csrfToken").doesNotExist());
    }

    @Test
    void login_InvalidCredentials_ReturnsBadRequest() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");

        when(authenticationService.authenticateUser(anyString(), anyString()))
            .thenThrow(new IllegalArgumentException("Invalid username or password"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void login_EmptyUsername_ReturnsBadRequest() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("", "password123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void login_EmptyPassword_ReturnsBadRequest() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
