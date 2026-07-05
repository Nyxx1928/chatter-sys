package org.example.chat.service;

import org.example.chat.entity.PendingRegistration;
import org.example.chat.entity.User;
import org.example.chat.repository.PendingRegistrationRepository;
import org.example.chat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BrevoEmailService brevoEmailService;

    private PasswordEncoder passwordEncoder;
    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        registrationService = new RegistrationService(
                pendingRegistrationRepository, userRepository, passwordEncoder, brevoEmailService);
    }

    // ── initiateRegistration ──────────────────────────────────────────────────

    @Test
    void initiateRegistration_ValidRequest_CreatesPendingWithOtpHash() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(pendingRegistrationRepository.existsByUsername("testuser")).thenReturn(false);
        when(pendingRegistrationRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(pendingRegistrationRepository.save(any(PendingRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(brevoEmailService.isOperational()).thenReturn(false);

        RegistrationService.RegistrationInitiationResult result =
                registrationService.initiateRegistration("testuser", "test@example.com", "TestP@ss1", "Test User");

        assertFalse(result.emailSent());
        assertEquals("Email service not configured", result.errorMessage());

        ArgumentCaptor<PendingRegistration> captor = ArgumentCaptor.forClass(PendingRegistration.class);
        verify(pendingRegistrationRepository).save(captor.capture());
        PendingRegistration saved = captor.getValue();

        assertEquals("testuser", saved.getUsername());
        assertEquals("test@example.com", saved.getEmail());
        assertEquals("Test User", saved.getDisplayName());
        assertNotNull(saved.getOtpHash());
        assertNotNull(saved.getOtpExpiry());
        assertTrue(saved.getOtpExpiry().isAfter(LocalDateTime.now()));
        assertEquals(0, saved.getAttemptCount());
    }

    @Test
    void initiateRegistration_ExistingPendingEmail_DeletesOldAndCreatesNew() {
        PendingRegistration existing = new PendingRegistration();
        existing.setEmail("test@example.com");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(pendingRegistrationRepository.existsByUsername("testuser")).thenReturn(false);
        when(pendingRegistrationRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existing));
        when(pendingRegistrationRepository.save(any(PendingRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(brevoEmailService.isOperational()).thenReturn(false);

        registrationService.initiateRegistration("testuser", "test@example.com", "TestP@ss1", "Test User");

        verify(pendingRegistrationRepository).delete(existing);
        verify(pendingRegistrationRepository).save(any(PendingRegistration.class));
    }

    @Test
    void initiateRegistration_DuplicateUsername_Throws() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> registrationService.initiateRegistration("testuser", "test@example.com", "TestP@ss1", "Test User"));
    }

    @Test
    void initiateRegistration_EmailAlreadyInUsers_Throws() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(pendingRegistrationRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> registrationService.initiateRegistration("testuser", "test@example.com", "TestP@ss1", "Test User"));
    }

    @Test
    void initiateRegistration_EmailServiceOperational_ReturnsEmailSent() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(pendingRegistrationRepository.existsByUsername("testuser")).thenReturn(false);
        when(pendingRegistrationRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        PendingRegistration saved = new PendingRegistration();
        saved.setId(1L);
        when(pendingRegistrationRepository.save(any(PendingRegistration.class))).thenReturn(saved);
        when(brevoEmailService.isOperational()).thenReturn(true);

        RegistrationService.RegistrationInitiationResult result =
                registrationService.initiateRegistration("testuser", "test@example.com", "TestP@ss1", "Test User");

        assertTrue(result.emailSent());
        assertNull(result.errorMessage());
        verify(brevoEmailService).isOperational();
    }

    // ── verifyOtp ─────────────────────────────────────────────────────────────

    @Test
    void verifyOtp_ValidOtp_CreatesUserAndReturnsSuccess() {
        String rawOtp = "123456";
        String otpHash = passwordEncoder.encode(rawOtp);
        PendingRegistration pending = createPendingRegistration("testuser", "test@example.com", otpHash,
                LocalDateTime.now().plusMinutes(10), 0);

        when(pendingRegistrationRepository.findByEmail("test@example.com")).thenReturn(Optional.of(pending));
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationService.OtpVerificationResult result =
                registrationService.verifyOtp("test@example.com", rawOtp);

        assertTrue(result.success());
        assertEquals("Email verified successfully", result.message());
        verify(pendingRegistrationRepository).delete(pending);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void verifyOtp_WrongOtp_ReturnsFailureAndIncrementsAttempts() {
        String rawOtp = "123456";
        String otpHash = passwordEncoder.encode(rawOtp);
        PendingRegistration pending = createPendingRegistration("testuser", "test@example.com", otpHash,
                LocalDateTime.now().plusMinutes(10), 0);

        when(pendingRegistrationRepository.findByEmail("test@example.com")).thenReturn(Optional.of(pending));
        when(pendingRegistrationRepository.save(any(PendingRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationService.OtpVerificationResult result =
                registrationService.verifyOtp("test@example.com", "654321");

        assertFalse(result.success());
        assertEquals("Invalid code. 2 attempts remaining.", result.message());
        assertEquals(1, pending.getAttemptCount());
        verify(pendingRegistrationRepository, never()).delete(any());
    }

    @Test
    void verifyOtp_MaxAttemptsExceeded_DeletesPendingAndReturnsFailure() {
        String otpHash = passwordEncoder.encode("123456");
        PendingRegistration pending = createPendingRegistration("testuser", "test@example.com", otpHash,
                LocalDateTime.now().plusMinutes(10), 3);

        when(pendingRegistrationRepository.findByEmail("test@example.com")).thenReturn(Optional.of(pending));

        RegistrationService.OtpVerificationResult result =
                registrationService.verifyOtp("test@example.com", "654321");

        assertFalse(result.success());
        assertEquals("Too many attempts", result.message());
        verify(pendingRegistrationRepository).delete(pending);
    }

    @Test
    void verifyOtp_WrongOtpReachesMaxAttempts_DeletesAndReturnsFailure() {
        String otpHash = passwordEncoder.encode("123456");
        PendingRegistration pending = createPendingRegistration("testuser", "test@example.com", otpHash,
                LocalDateTime.now().plusMinutes(10), 2);

        when(pendingRegistrationRepository.findByEmail("test@example.com")).thenReturn(Optional.of(pending));
        when(pendingRegistrationRepository.save(any(PendingRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationService.OtpVerificationResult result =
                registrationService.verifyOtp("test@example.com", "654321");

        assertFalse(result.success());
        assertEquals("Too many attempts", result.message());
        verify(pendingRegistrationRepository).delete(pending);
    }

    @Test
    void verifyOtp_Expired_DeletesAndReturnsFailure() {
        PendingRegistration pending = createPendingRegistration("testuser", "test@example.com", "hash",
                LocalDateTime.now().minusMinutes(1), 0);

        when(pendingRegistrationRepository.findByEmail("test@example.com")).thenReturn(Optional.of(pending));

        RegistrationService.OtpVerificationResult result =
                registrationService.verifyOtp("test@example.com", "123456");

        assertFalse(result.success());
        assertEquals("Code expired", result.message());
        verify(pendingRegistrationRepository).delete(pending);
    }

    @Test
    void verifyOtp_NotFound_ReturnsFailure() {
        when(pendingRegistrationRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        RegistrationService.OtpVerificationResult result =
                registrationService.verifyOtp("unknown@example.com", "123456");

        assertFalse(result.success());
        assertEquals("Invalid code", result.message());
    }

    // ── resendOtp ──────────────────────────────────────────────────────────────

    @Test
    void resendOtp_ExistingPending_OverwritesOtpAndResetsAttempts() {
        String oldHash = passwordEncoder.encode("000000");
        PendingRegistration pending = createPendingRegistration("testuser", "test@example.com", oldHash,
                LocalDateTime.now().minusMinutes(1), 2);
        LocalDateTime oldExpiry = pending.getOtpExpiry();

        when(pendingRegistrationRepository.findByEmail("test@example.com")).thenReturn(Optional.of(pending));
        when(pendingRegistrationRepository.save(any(PendingRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(brevoEmailService.isOperational()).thenReturn(false);

        registrationService.resendOtp("test@example.com");

        verify(pendingRegistrationRepository).save(pending);
        assertNotEquals(oldHash, pending.getOtpHash());
        assertTrue(pending.getOtpExpiry().isAfter(oldExpiry));
        assertEquals(0, pending.getAttemptCount());
    }

    @Test
    void resendOtp_NotFound_ReturnsSilently() {
        when(pendingRegistrationRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        registrationService.resendOtp("unknown@example.com");

        verify(pendingRegistrationRepository, never()).save(any());
        verify(brevoEmailService, never()).sendOtpEmail(anyString(), anyString());
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private PendingRegistration createPendingRegistration(String username, String email, String otpHash,
                                                           LocalDateTime otpExpiry, int attemptCount) {
        PendingRegistration pending = new PendingRegistration();
        pending.setUsername(username);
        pending.setEmail(email);
        pending.setPasswordHash("$2a$10$dummyhash");
        pending.setDisplayName("Test User");
        pending.setOtpHash(otpHash);
        pending.setOtpExpiry(otpExpiry);
        pending.setAttemptCount(attemptCount);
        pending.setCreatedAt(LocalDateTime.now());
        return pending;
    }
}
