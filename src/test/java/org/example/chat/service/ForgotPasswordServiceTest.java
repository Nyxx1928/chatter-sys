package org.example.chat.service;

import org.example.chat.entity.PasswordResetToken;
import org.example.chat.entity.User;
import org.example.chat.exception.RateLimitExceededException;
import org.example.chat.repository.PasswordResetTokenRepository;
import org.example.chat.repository.UserRepository;
import org.example.chat.util.SecurityAuditLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private SecurityAuditLogger auditLogger;

    @InjectMocks
    private ForgotPasswordService forgotPasswordService;

    private User testUser;
    private PasswordResetToken validToken;
    private PasswordResetToken expiredToken;
    private PasswordResetToken usedToken;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("old-hashed-password");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setOnline(false);

        validToken = new PasswordResetToken(testUser, "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789");
        ReflectionTestUtils.setField(validToken, "id", 1L);
        ReflectionTestUtils.setField(validToken, "expiryDate", LocalDateTime.now().plusMinutes(15));
        ReflectionTestUtils.setField(validToken, "used", false);
        ReflectionTestUtils.setField(validToken, "createdAt", LocalDateTime.now());

        expiredToken = new PasswordResetToken(testUser, "expired-token-hex-1234567890abcdef1234567890abcdef");
        ReflectionTestUtils.setField(expiredToken, "id", 2L);
        ReflectionTestUtils.setField(expiredToken, "expiryDate", LocalDateTime.now().minusMinutes(1));
        ReflectionTestUtils.setField(expiredToken, "used", false);
        ReflectionTestUtils.setField(expiredToken, "createdAt", LocalDateTime.now().minusMinutes(20));

        usedToken = new PasswordResetToken(testUser, "used-token-hex-1234567890abcdef1234567890abcdef");
        ReflectionTestUtils.setField(usedToken, "id", 3L);
        ReflectionTestUtils.setField(usedToken, "expiryDate", LocalDateTime.now().plusMinutes(15));
        ReflectionTestUtils.setField(usedToken, "used", true);
        ReflectionTestUtils.setField(usedToken, "createdAt", LocalDateTime.now());

        ReflectionTestUtils.setField(forgotPasswordService, "frontendUrl", "http://localhost:3000");
    }

    @Test
    void initiateReset_RegisteredEmail_CreatesTokenAndSendsEmail() {
        String email = "test@example.com";

        doNothing().when(rateLimiterService).checkForgotPassword(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenReturn(validToken);
        when(emailService.sendPasswordResetEmail(anyString(), anyString(), anyString())).thenReturn(true);

        forgotPasswordService.initiateReset(email);

        verify(rateLimiterService).checkForgotPassword(email);
        verify(userRepository).findByEmail(email);
        verify(tokenRepository).deleteByUser(testUser);
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq("test@example.com"), anyString(), eq("testuser"));
    }

    @Test
    void initiateReset_UnregisteredEmail_ReturnsSilently() {
        String email = "unknown@example.com";

        doNothing().when(rateLimiterService).checkForgotPassword(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        forgotPasswordService.initiateReset(email);

        verify(rateLimiterService).checkForgotPassword(email);
        verify(userRepository).findByEmail(email);
        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    void initiateReset_RateLimitExceeded_ThrowsException() {
        String email = "test@example.com";

        doThrow(new RateLimitExceededException("Too many requests"))
                .when(rateLimiterService).checkForgotPassword(email);

        assertThrows(RateLimitExceededException.class,
                () -> forgotPasswordService.initiateReset(email));

        verify(userRepository, never()).findByEmail(anyString());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void resetPassword_ValidToken_UpdatesPassword() {
        String tokenStr = validToken.getToken();
        String newPassword = "newSecurePassword123";

        when(tokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(validToken));
        when(passwordEncoder.encode(newPassword)).thenReturn("new-encoded-password");

        forgotPasswordService.resetPassword(tokenStr, newPassword);

        assertEquals("new-encoded-password", testUser.getPasswordHash());
        assertTrue(validToken.getUsed());
        verify(userRepository).save(testUser);
        verify(tokenRepository).save(validToken);
    }

    @Test
    void resetPassword_ExpiredToken_ThrowsException() {
        String tokenStr = "non-existent-token";
        when(tokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(expiredToken));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> forgotPasswordService.resetPassword(tokenStr, "newPassword123"));

        assertTrue(exception.getMessage().contains("expired"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_UsedToken_ThrowsException() {
        String tokenStr = usedToken.getToken();
        when(tokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(usedToken));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> forgotPasswordService.resetPassword(tokenStr, "newPassword123"));

        assertTrue(exception.getMessage().contains("used"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_InvalidToken_ThrowsException() {
        String tokenStr = "nonexistent-token";
        when(tokenRepository.findByToken(tokenStr)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> forgotPasswordService.resetPassword(tokenStr, "newPassword123"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_NullToken_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> forgotPasswordService.resetPassword(null, "newPassword123"));
    }

    @Test
    void resetPassword_BlankToken_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> forgotPasswordService.resetPassword("", "newPassword123"));
    }
}
