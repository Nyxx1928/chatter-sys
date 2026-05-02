package org.example.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.chat.config.WebMvcTestConfig;
import org.example.chat.dto.UpdateProfileRequest;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for UserController.
 */
@WebMvcTest(controllers = UserController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
@Import({WebMvcTestConfig.class, org.example.chat.exception.GlobalExceptionHandler.class}) // Import test config and exception handler
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for unit tests
@ActiveProfiles("test") // Use H2 in-memory database for tests
class UserControllerTest {

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
    @WithMockUser(username = "testuser")
    void getCurrentUser_Authenticated_ReturnsUser() throws Exception {
        // Arrange
        when(authenticationService.getUserByUsername("testuser"))
            .thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.displayName").value("Test User"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateCurrentUser_ValidRequest_ReturnsUpdatedUser() throws Exception {
        // Arrange
        UpdateProfileRequest request = new UpdateProfileRequest(
            "newemail@example.com",
            "New Display Name"
        );

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setEmail("newemail@example.com");
        updatedUser.setDisplayName("New Display Name");
        updatedUser.setCreatedAt(testUser.getCreatedAt());
        updatedUser.setOnline(false);

        when(authenticationService.updateUserProfile(
            eq("testuser"),
            eq("newemail@example.com"),
            eq("New Display Name")
        )).thenReturn(updatedUser);

        // Act & Assert
        mockMvc.perform(put("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("newemail@example.com"))
            .andExpect(jsonPath("$.displayName").value("New Display Name"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateCurrentUser_InvalidEmail_ReturnsBadRequest() throws Exception {
        // Arrange
        UpdateProfileRequest request = new UpdateProfileRequest(
            "invalid-email",  // Invalid email format
            "New Display Name"
        );

        // Act & Assert
        mockMvc.perform(put("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateCurrentUser_DuplicateEmail_ReturnsBadRequest() throws Exception {
        // Arrange
        UpdateProfileRequest request = new UpdateProfileRequest(
            "existing@example.com",
            "New Display Name"
        );

        when(authenticationService.updateUserProfile(
            anyString(), anyString(), anyString()
        )).thenThrow(new IllegalArgumentException("Email already exists"));

        // Act & Assert
        mockMvc.perform(put("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateCurrentUser_EmptyRequest_ReturnsOk() throws Exception {
        // Arrange - empty request should not update anything
        UpdateProfileRequest request = new UpdateProfileRequest(null, null);

        when(authenticationService.updateUserProfile(
            eq("testuser"), eq(null), eq(null)
        )).thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(put("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("testuser"));
    }
}
