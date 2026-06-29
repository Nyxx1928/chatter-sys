package org.example.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.chat.config.WebMvcTestConfig;
import org.example.chat.dto.ForgotPasswordRequest;
import org.example.chat.dto.LoginRequest;
import org.example.chat.dto.RegisterRequest;
import org.example.chat.dto.ResetPasswordRequest;
import org.example.chat.entity.User;
import org.example.chat.service.AuthenticationService;
import org.example.chat.service.ForgotPasswordService;
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
import static org.mockito.Mockito.*;
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

    @MockBean
    private ForgotPasswordService forgotPasswordService;

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
            "TestP@ss1",  // Meets complexity: uppercase, lowercase, digit, special char
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
            "TestP@ss1",  // Meets complexity: uppercase, lowercase, digit, special char
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

    @Test
    void forgotPassword_ValidEmail_ReturnsOk() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");

        doNothing().when(forgotPasswordService).initiateReset("test@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        verify(forgotPasswordService).initiateReset("test@example.com");
    }

    @Test
    void forgotPassword_InvalidEmail_ReturnsBadRequest() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("invalid-email");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(forgotPasswordService, never()).initiateReset(anyString());
    }

    @Test
    void resetPassword_ValidRequest_ReturnsOk() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("valid-token-hex", "newPassword123");

        doNothing().when(forgotPasswordService).resetPassword("valid-token-hex", "newPassword123");

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        verify(forgotPasswordService).resetPassword("valid-token-hex", "newPassword123");
    }

    @Test
    void resetPassword_WeakPassword_ReturnsBadRequest() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("valid-token-hex", "short");

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(forgotPasswordService, never()).resetPassword(anyString(), anyString());
    }

    @Test
    void resetPassword_ExpiredToken_ReturnsBadRequest() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("expired-token-hex", "newPassword123");

        doThrow(new IllegalArgumentException("This reset link has expired. Please request a new one."))
                .when(forgotPasswordService).resetPassword("expired-token-hex", "newPassword123");

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("This reset link has expired. Please request a new one."));
    }
}
