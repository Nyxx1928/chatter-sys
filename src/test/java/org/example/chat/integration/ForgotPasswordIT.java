package org.example.chat.integration;

import org.example.chat.dto.ForgotPasswordRequest;
import org.example.chat.dto.LoginRequest;
import org.example.chat.dto.RegisterRequest;
import org.example.chat.dto.ResetPasswordRequest;
import org.example.chat.entity.PasswordResetToken;
import org.example.chat.entity.User;
import org.example.chat.repository.PasswordResetTokenRepository;
import org.example.chat.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ForgotPasswordIT extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User createVerifiedUser() {
        User user = new User();
        user.setUsername("resetuser");
        user.setEmail("resetuser@example.com");
        user.setPasswordHash(passwordEncoder.encode("originalPass123"));
        user.setDisplayName("Reset User");
        user.setCreatedAt(LocalDateTime.now());
        user.setOnline(false);
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    @Test
    void forgotPassword_RegisteredEmail_ReturnsOk() throws Exception {
        createVerifiedUser();

        ForgotPasswordRequest request = new ForgotPasswordRequest("resetuser@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
            .andExpect(status().isOk());
    }

    @Test
    void forgotPassword_UnregisteredEmail_ReturnsOk() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("unknown@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
            .andExpect(status().isOk());
    }

    @Test
    void forgotPassword_InvalidEmail_ReturnsBadRequest() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("not-an-email");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_ValidToken_UpdatesPasswordAndAllowsLogin() throws Exception {
        User user = createVerifiedUser();

        PasswordResetToken token = new PasswordResetToken(user, "valid-integration-token-hex-1234567890abcdef1234");
        tokenRepository.save(token);

        ResetPasswordRequest resetRequest = new ResetPasswordRequest(
                token.getToken(), "newSecurePassword456");

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(resetRequest)))
            .andExpect(status().isOk());

        PasswordResetToken savedToken = tokenRepository.findByToken(token.getToken()).orElse(null);
        assertNotNull(savedToken);
        assertTrue(savedToken.getUsed());

        User updatedUser = userRepository.findByUsername("resetuser").orElse(null);
        assertNotNull(updatedUser);
        assertTrue(passwordEncoder.matches("newSecurePassword456", updatedUser.getPasswordHash()));

        LoginRequest loginRequest = new LoginRequest("resetuser", "newSecurePassword456");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void resetPassword_ExpiredToken_ReturnsBadRequest() throws Exception {
        User user = createVerifiedUser();

        PasswordResetToken expiredToken = new PasswordResetToken(user, "expired-integration-token-hex-1234567890abcdef");
        tokenRepository.save(expiredToken);
        expiredToken.setExpiryDate(LocalDateTime.now().minusMinutes(5));
        tokenRepository.save(expiredToken);

        ResetPasswordRequest request = new ResetPasswordRequest(
                expiredToken.getToken(), "newPassword456");

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("expired")));
    }

    @Test
    void resetPassword_UsedToken_ReturnsBadRequest() throws Exception {
        User user = createVerifiedUser();

        PasswordResetToken usedToken = new PasswordResetToken(user, "used-integration-token-hex-1234567890abcdef");
        usedToken.setUsed(true);
        tokenRepository.save(usedToken);

        ResetPasswordRequest request = new ResetPasswordRequest(
                usedToken.getToken(), "newPassword456");

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("used")));
    }

    @Test
    void resetPassword_InvalidToken_ReturnsBadRequest() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "nonexistent-token-hex-1234567890abcdef12345678", "newPassword456");

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_WeakPassword_ReturnsBadRequest() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "some-token-hex-1234567890abcdef1234567890abcdef", "short");

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
            .andExpect(status().isBadRequest());
    }
}
