package org.example.chat.service;

import org.example.chat.entity.PendingRegistration;
import org.example.chat.entity.User;
import org.example.chat.repository.PendingRegistrationRepository;
import org.example.chat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Service for handling user registration with OTP email verification.
 * Implements verify-first registration: email must be verified before account is created.
 */
@Service
public class RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BrevoEmailService brevoEmailService;

    public RegistrationService(
            PendingRegistrationRepository pendingRegistrationRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            BrevoEmailService brevoEmailService) {
        this.pendingRegistrationRepository = pendingRegistrationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.brevoEmailService = brevoEmailService;
    }

    /**
     * Initiates user registration by creating a pending registration and sending OTP email.
     * The user account is NOT created until email is verified.
     *
     * @param username the desired username
     * @param email the user's email address
     * @param password the plain text password
     * @param displayName the user's display name
     * @return RegistrationInitiationResult containing status details
     * @throws IllegalArgumentException if validation fails or username/email already exists
     */
    @Transactional
    public RegistrationInitiationResult initiateRegistration(
            String username, String email, String password, String displayName) {

        logger.info("Initiating registration for username: {}, email: {}", username, email);

        validateRegistrationInput(username, email, password, displayName);

        if (userRepository.existsByUsername(username) ||
            pendingRegistrationRepository.existsByUsername(username)) {
            logger.warn("Registration failed: username already exists or pending: {}", username);
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(email) || pendingRegistrationRepository.existsByEmail(email)) {
            logger.warn("Registration failed: email already exists or is pending: {}", email);
            throw new IllegalArgumentException("Email already exists");
        }

        String passwordHash = passwordEncoder.encode(password);
        String otp = generateOtp();
        String otpHash = passwordEncoder.encode(otp);
        LocalDateTime otpExpiry = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        PendingRegistration pending = new PendingRegistration(
                username, email, passwordHash, displayName, otpHash, otpExpiry);
        PendingRegistration savedPending = pendingRegistrationRepository.save(pending);
        final Long pendingId = savedPending.getId();

        boolean emailWillBeSent = brevoEmailService.isOperational();

        if (emailWillBeSent) {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    BrevoEmailService.EmailResult emailResult =
                            brevoEmailService.sendOtpEmail(email, otp);
                    PendingRegistration pr = pendingRegistrationRepository.findById(pendingId).orElse(null);
                    if (pr != null) {
                        pr.setEmailSent(emailResult.success());
                        pendingRegistrationRepository.save(pr);
                    }
                    if (emailResult.success()) {
                        logger.info("OTP email sent successfully to: {}", email);
                    } else {
                        logger.error("Failed to send OTP email to {}: {}", email, emailResult.errorMessage());
                    }
                } catch (Exception e) {
                    logger.error("Async OTP email failed for {}: {}", email, e.getMessage(), e);
                }
            });
            logger.info("Pending registration created for username: {}, OTP will be sent asynchronously", username);
        } else {
            logger.info("Pending registration created for username: {}, email service not configured", username);
        }

        return new RegistrationInitiationResult(
                emailWillBeSent,
                emailWillBeSent ? null : "Email service not configured"
        );
    }

    /**
     * Verifies the OTP and completes registration by creating the user account.
     *
     * @param email the email address
     * @param rawOtp the 6-digit OTP to verify
     * @return OtpVerificationResult indicating success/failure with a message
     */
    @Transactional
    public OtpVerificationResult verifyOtp(String email, String rawOtp) {
        logger.info("Verifying OTP for email: {}", email);

        PendingRegistration pending = pendingRegistrationRepository.findByEmail(email).orElse(null);
        if (pending == null) {
            logger.warn("OTP verification failed: no pending registration for email: {}", email);
            return new OtpVerificationResult(false, "Invalid code");
        }

        if (pending.isExpired()) {
            logger.warn("OTP expired for email: {}", email);
            pendingRegistrationRepository.delete(pending);
            return new OtpVerificationResult(false, "Code expired");
        }

        if (pending.isMaxAttemptsExceeded()) {
            logger.warn("Max OTP attempts exceeded for email: {}", email);
            pendingRegistrationRepository.delete(pending);
            return new OtpVerificationResult(false, "Too many attempts");
        }

        if (!passwordEncoder.matches(rawOtp, pending.getOtpHash())) {
            pending.incrementAttempts();
            pendingRegistrationRepository.save(pending);

            if (pending.isMaxAttemptsExceeded()) {
                logger.warn("Max OTP attempts exceeded for email: {}", email);
                pendingRegistrationRepository.delete(pending);
                return new OtpVerificationResult(false, "Too many attempts");
            }

            int remaining = 3 - pending.getAttemptCount();
            logger.warn("Invalid OTP for email: {} ({} attempts remaining)", email, remaining);
            return new OtpVerificationResult(false, "Invalid code. " + remaining + " attempts remaining.");
        }

        // Check if username or email was taken while pending
        if (userRepository.existsByUsername(pending.getUsername())) {
            logger.warn("Username taken during pending period: {}", pending.getUsername());
            pendingRegistrationRepository.delete(pending);
            return new OtpVerificationResult(false, "Username is no longer available");
        }

        if (userRepository.existsByEmail(pending.getEmail())) {
            logger.warn("Email taken during pending period: {}", pending.getEmail());
            pendingRegistrationRepository.delete(pending);
            return new OtpVerificationResult(false, "Email is no longer available");
        }

        User user = new User();
        user.setUsername(pending.getUsername());
        user.setEmail(pending.getEmail());
        user.setPasswordHash(pending.getPasswordHash());
        user.setDisplayName(pending.getDisplayName());
        user.setCreatedAt(LocalDateTime.now());
        user.setOnline(false);
        user.setEmailVerified(true);

        userRepository.save(user);
        pendingRegistrationRepository.delete(pending);

        logger.info("Registration completed successfully for username: {}", user.getUsername());
        return new OtpVerificationResult(true, "Email verified successfully");
    }

    /**
     * Resends a new OTP for a pending registration.
     *
     * @param email the email address of the pending registration
     */
    @Transactional
    public void resendOtp(String email) {
        logger.info("Resending OTP to: {}", email);

        PendingRegistration pending = pendingRegistrationRepository.findByEmail(email).orElse(null);
        if (pending == null) {
            logger.info("No pending registration found for email: {} (returning silently)", email);
            return;
        }

        String newOtp = generateOtp();
        String newOtpHash = passwordEncoder.encode(newOtp);
        pending.setOtpHash(newOtpHash);
        pending.setOtpExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        pending.setAttemptCount(0);
        pendingRegistrationRepository.save(pending);

        if (brevoEmailService.isOperational()) {
            BrevoEmailService.EmailResult emailResult =
                    brevoEmailService.sendOtpEmail(email, newOtp);
            pending.setEmailSent(emailResult.success());
            pendingRegistrationRepository.save(pending);
            if (emailResult.success()) {
                logger.info("OTP resent successfully to: {}", email);
            } else {
                logger.error("Failed to resend OTP to {}: {}", email, emailResult.errorMessage());
            }
        } else {
            logger.info("Email service not configured, OTP not sent for: {}", email);
        }
    }

    /**
     * Cleans up expired pending registrations.
     * Runs every hour.
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    @Transactional
    public void cleanupExpiredPendingRegistrations() {
        LocalDateTime cutoff = LocalDateTime.now();
        pendingRegistrationRepository.deleteByOtpExpiryBefore(cutoff);
        logger.info("Cleaned up expired pending registrations older than {}", cutoff);
    }

    private String generateOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1000000));
    }

    private void validateRegistrationInput(String username, String email, String password, String displayName) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        if (username.length() > 50) {
            throw new IllegalArgumentException("Username cannot exceed 50 characters");
        }

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }

        if (email.length() > 100) {
            throw new IllegalArgumentException("Email cannot exceed 100 characters");
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        if (!password.matches("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*#?&]).{8,}$")) {
            throw new IllegalArgumentException(
                    "Password must contain at least one uppercase letter, one lowercase letter, " +
                    "one digit, and one special character (@$!%*#?&)");
        }

        if (displayName == null || displayName.trim().isEmpty()) {
            throw new IllegalArgumentException("Display name cannot be empty");
        }

        if (displayName.length() > 100) {
            throw new IllegalArgumentException("Display name cannot exceed 100 characters");
        }
    }

    /**
     * Result of registration initiation.
     *
     * @param emailSent whether the email was sent successfully
     * @param errorMessage error message if email failed, null otherwise
     */
    public record RegistrationInitiationResult(
            boolean emailSent,
            String errorMessage
    ) {}

    /**
     * Result of OTP verification.
     *
     * @param success whether verification succeeded
     * @param message result message
     */
    public record OtpVerificationResult(
            boolean success,
            String message
    ) {}
}
