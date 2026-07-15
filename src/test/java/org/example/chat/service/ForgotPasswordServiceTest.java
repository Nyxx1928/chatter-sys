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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
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
    private BrevoEmailService brevoEmailService;

    @Mock
    private SecurityAuditLogger auditLogger;

    @InjectMocks
    private ForgotPasswordService forgotPasswordService;

    private User testUser;
    private PasswordResetToken validToken;
    private PasswordResetToken expiredToken;
    private PasswordResetToken usedToken;
    private String rawValidToken;
    private String rawExpiredToken;
    private String rawUsedToken;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("old-hashed-password");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setOnline(false);

        this.rawValidToken = "raw-valid-token-abcdef0123456789abcdef0123456789";
        this.rawExpiredToken = "raw-expired-token-hex-1234567890abcdef123456";
        this.rawUsedToken = "raw-used-token-hex-1234567890abcdef1234567890";

        validToken = new PasswordResetToken(testUser, sha256(rawValidToken));
        ReflectionTestUtils.setField(validToken, "id", 1L);
        ReflectionTestUtils.setField(validToken, "expiryDate", LocalDateTime.now().plusMinutes(15));
        ReflectionTestUtils.setField(validToken, "used", false);
        ReflectionTestUtils.setField(validToken, "createdAt", LocalDateTime.now());

        expiredToken = new PasswordResetToken(testUser, sha256(rawExpiredToken));
        ReflectionTestUtils.setField(expiredToken, "id", 2L);
        ReflectionTestUtils.setField(expiredToken, "expiryDate", LocalDateTime.now().minusMinutes(1));
        ReflectionTestUtils.setField(expiredToken, "used", false);
        ReflectionTestUtils.setField(expiredToken, "createdAt", LocalDateTime.now().minusMinutes(20));

        usedToken = new PasswordResetToken(testUser, sha256(rawUsedToken));
        ReflectionTestUtils.setField(usedToken, "id", 3L);
        ReflectionTestUtils.setField(usedToken, "expiryDate", LocalDateTime.now().plusMinutes(15));
        ReflectionTestUtils.setField(usedToken, "used", true);
        ReflectionTestUtils.setField(usedToken, "createdAt", LocalDateTime.now());

        ReflectionTestUtils.setField(forgotPasswordService, "frontendUrl", "http://localhost:3000");
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void initiateReset_RegisteredEmail_CreatesTokenAndSendsEmail() {
        String email = "test@example.com";

        doNothing().when(rateLimiterService).checkForgotPassword(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenReturn(validToken);
        when(brevoEmailService.sendPasswordResetEmail(anyString(), anyString(), anyString()))
                .thenReturn(new BrevoEmailService.EmailResult(true, null, "msg-123"));

        forgotPasswordService.initiateReset(email);

        verify(rateLimiterService).checkForgotPassword(email);
        verify(userRepository).findByEmail(email);
        verify(tokenRepository).deleteByUser(testUser);
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(brevoEmailService).sendPasswordResetEmail(eq("test@example.com"), anyString(), eq("testuser"));
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
        verify(brevoEmailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
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
        String tokenStr = rawValidToken;
        String newPassword = "newSecurePassword123";

        when(tokenRepository.findByToken(sha256(tokenStr))).thenReturn(Optional.of(validToken));
        when(passwordEncoder.encode(newPassword)).thenReturn("new-encoded-password");

        forgotPasswordService.resetPassword(tokenStr, newPassword);

        assertEquals("new-encoded-password", testUser.getPasswordHash());
        assertTrue(validToken.getUsed());
        verify(userRepository).save(testUser);
        verify(tokenRepository).save(validToken);
    }

    @Test
    void resetPassword_ExpiredToken_ThrowsException() {
        String tokenStr = rawExpiredToken;
        when(tokenRepository.findByToken(sha256(tokenStr))).thenReturn(Optional.of(expiredToken));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> forgotPasswordService.resetPassword(tokenStr, "newPassword123"));

        assertTrue(exception.getMessage().contains("expired"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_UsedToken_ThrowsException() {
        String tokenStr = rawUsedToken;
        when(tokenRepository.findByToken(sha256(tokenStr))).thenReturn(Optional.of(usedToken));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> forgotPasswordService.resetPassword(tokenStr, "newPassword123"));

        assertTrue(exception.getMessage().contains("used"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_InvalidToken_ThrowsException() {
        String tokenStr = "nonexistent-token";
        when(tokenRepository.findByToken(sha256(tokenStr))).thenReturn(Optional.empty());

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
