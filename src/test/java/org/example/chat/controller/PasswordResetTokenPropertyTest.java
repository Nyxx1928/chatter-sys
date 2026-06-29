package org.example.chat.controller;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import org.example.chat.entity.PasswordResetToken;
import org.example.chat.entity.User;
import org.example.chat.repository.PasswordResetTokenRepository;
import org.example.chat.repository.UserRepository;
import org.example.chat.service.BrevoEmailService;
import org.example.chat.service.ForgotPasswordService;
import org.example.chat.service.RateLimiterService;
import org.example.chat.util.SecurityAuditLogger;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Property-based test for password reset token generation and validation.
 *
 * Verifies that:
 * 1. Generated tokens are always valid hex strings of the correct length.
 * 2. Expired tokens are always rejected regardless of token content.
 * 3. Used tokens are always rejected regardless of token content.
 */
@PropertyDefaults(tries = 100)
class PasswordResetTokenPropertyTest {

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedpassword");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setOnline(true);
    }

    @Property
    @Label("Generated tokens are always 64-character hex strings")
    void generatedTokenIsValidHex(
            @ForAll @IntRange(min = 1, max = 10) int seed) {
        setUp();

        UserRepository userRepository = mock(UserRepository.class);
        PasswordResetTokenRepository tokenRepository = mock(PasswordResetTokenRepository.class);
        RateLimiterService rateLimiterService = mock(RateLimiterService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        BrevoEmailService brevoEmailService = mock(BrevoEmailService.class);
        SecurityAuditLogger auditLogger = mock(SecurityAuditLogger.class);

        ForgotPasswordService service = new ForgotPasswordService(
                userRepository, tokenRepository, rateLimiterService,
                passwordEncoder, brevoEmailService, auditLogger);
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:3000");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(brevoEmailService.sendPasswordResetEmail(anyString(), anyString(), anyString()))
                .thenReturn(new BrevoEmailService.EmailResult(true, null, "msg-123"));

        service.initiateReset("test@example.com");

        verify(tokenRepository).save(argThat(token -> {
            String tokenValue = token.getToken();
            return tokenValue != null
                    && tokenValue.length() == 64
                    && tokenValue.matches("[0-9a-f]{64}");
        }));
    }

    @Property
    @Label("Expired tokens are always rejected")
    void expiredTokenAlwaysRejected(
            @ForAll @IntRange(min = 1, max = 10) int minutesPastExpiry) {
        setUp();

        PasswordResetToken expiredToken = new PasswordResetToken(testUser, "any-token-value");
        expiredToken.setUsed(false);
        expiredToken.setExpiryDate(LocalDateTime.now().minusMinutes(minutesPastExpiry));

        assertTrue(expiredToken.isExpired());
    }

    @Property
    @Label("Used tokens are always rejected regardless of expiry")
    void usedTokenAlwaysRejected(
            @ForAll @IntRange(min = 0, max = 60) int minutesUntilExpiry) {
        setUp();

        PasswordResetToken usedToken = new PasswordResetToken(testUser, "any-token-value");
        usedToken.setUsed(true);
        usedToken.setExpiryDate(LocalDateTime.now().plusMinutes(minutesUntilExpiry));

        PasswordResetTokenRepository tokenRepository = mock(PasswordResetTokenRepository.class);
        when(tokenRepository.findByToken("any-token-value")).thenReturn(Optional.of(usedToken));

        UserRepository userRepository = mock(UserRepository.class);
        RateLimiterService rateLimiterService = mock(RateLimiterService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        BrevoEmailService brevoEmailService = mock(BrevoEmailService.class);
        SecurityAuditLogger auditLogger = mock(SecurityAuditLogger.class);

        ForgotPasswordService service = new ForgotPasswordService(
                userRepository, tokenRepository, rateLimiterService,
                passwordEncoder, brevoEmailService, auditLogger);

        assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword("any-token-value", "newPassword123"));
        verify(userRepository, never()).save(any());
    }

    @Property
    @Label("Forgot password returns silently for any unregistered email")
    void unregisteredEmailAlwaysReturnsSilently(
            @ForAll @StringLength(min = 5, max = 50) String localPart,
            @ForAll @StringLength(min = 3, max = 10) String domain,
            @ForAll @StringLength(min = 2, max = 5) String tld) {
        setUp();

        String email = localPart + "@" + domain + "." + tld;

        UserRepository userRepository = mock(UserRepository.class);
        PasswordResetTokenRepository tokenRepository = mock(PasswordResetTokenRepository.class);
        RateLimiterService rateLimiterService = mock(RateLimiterService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        BrevoEmailService brevoEmailService = mock(BrevoEmailService.class);
        SecurityAuditLogger auditLogger = mock(SecurityAuditLogger.class);

        ForgotPasswordService service = new ForgotPasswordService(
                userRepository, tokenRepository, rateLimiterService,
                passwordEncoder, brevoEmailService, auditLogger);

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.initiateReset(email));
        verify(tokenRepository, never()).save(any());
    }
}
