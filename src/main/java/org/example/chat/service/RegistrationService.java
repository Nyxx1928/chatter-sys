package org.example.chat.service;

import org.example.chat.entity.PendingRegistration;
import org.example.chat.entity.User;
import org.example.chat.repository.PendingRegistrationRepository;
import org.example.chat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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

        if (userRepository.existsByUsername(username)) {
            logger.warn("Registration failed: username already exists: {}", username);
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            logger.warn("Registration failed: email already exists: {}", email);
            throw new IllegalArgumentException("Email already exists");
        }

        // A pending registration for this email with a DIFFERENT username
        // means the email is already claimed.
        Optional<PendingRegistration> pendingByEmail = pendingRegistrationRepository.findByEmail(email);
        if (pendingByEmail.isPresent() && !username.equals(pendingByEmail.get().getUsername())) {
            logger.warn("Registration failed: email already has a pending registration: {}", email);
            throw new IllegalArgumentException("Email already exists");
        }
        // Same email + same username: the user is re-initiating registration —
        // replace the stale pending row instead of failing.
        // Flush immediately: with IDENTITY id-generation the new row is
        // inserted right away, so the old row must be deleted first or the
        // unique constraint on email/username is violated.
        if (pendingByEmail.isPresent()) {
            pendingRegistrationRepository.delete(pendingByEmail.get());
            pendingRegistrationRepository.flush();
            logger.info("Replacing existing pending registration for email: {}", email);
        }

        // Any remaining pending row with this username belongs to a different
        // email, so the username is taken.
        if (pendingRegistrationRepository.existsByUsername(username)) {
            logger.warn("Registration failed: username already has a pending registration: {}", username);
            throw new IllegalArgumentException("Username already exists");
        }

        String passwordHash = passwordEncoder.encode(password);
        String otp = generateOtp();
        String otpHash = passwordEncoder.encode(otp);
        LocalDateTime otpExpiry = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        PendingRegistration pending = new PendingRegistration(
                username, email, passwordHash, displayName, otpHash, otpExpiry);
        PendingRegistration savedPending;
        try {
            savedPending = pendingRegistrationRepository.save(pending);
        } catch (DataIntegrityViolationException e) {
            // Unique constraint on username/email — a concurrent registration won.
            logger.warn("Registration failed: concurrent pending registration for username {} or email {}",
                    username, email);
            throw new IllegalArgumentException("Username or email already has a pending registration");
        }
        final Long pendingId = savedPending.getId();

        boolean emailWillBeSent = brevoEmailService.isOperational();

        if (emailWillBeSent) {
            // Send AFTER the transaction commits: the pending row is not visible
            // to the async task until the commit is complete.
            scheduleOtpEmailAfterCommit(pendingId, email, otp);
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

        // Pessimistic lock: serializes concurrent verifications for the same
        // email so only one request can ever create the user account.
        PendingRegistration pending =
                pendingRegistrationRepository.findByEmailForUpdate(email).orElse(null);
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

            int remaining = pending.getRemainingAttempts();
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

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // DB unique constraint on username/email — a concurrent registration
            // claimed one of them between our check and the insert. The
            // transaction is unusable at this point, so fail fast.
            logger.warn("User creation failed for {}: unique constraint violated", pending.getUsername());
            throw new IllegalArgumentException("Username or email is no longer available");
        }
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
            // Send after commit so the email call never holds the DB transaction open.
            scheduleOtpEmailAfterCommit(pending.getId(), email, newOtp);
            logger.info("OTP resent asynchronously to: {}", email);
        } else {
            logger.info("Email service not configured, OTP not sent for: {}", email);
        }
    }

    /**
     * Schedules the OTP email so it is only sent after the surrounding
     * transaction commits. This guarantees the pending registration row is
     * visible to the async task (fixes the race where the task ran before
     * commit and silently lost the emailSent update).
     *
     * When there is no active transaction (e.g., unit tests), the email is
     * sent immediately.
     */
    private void scheduleOtpEmailAfterCommit(Long pendingId, String email, String otp) {
        Runnable sendEmail = () -> sendOtpEmailAsync(pendingId, email, otp);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    CompletableFuture.runAsync(sendEmail);
                }
            });
        } else {
            CompletableFuture.runAsync(sendEmail);
        }
    }

    /**
     * Sends the OTP email and records the delivery status on the pending
     * registration. Runs on a worker thread; failures are logged only.
     */
    private void sendOtpEmailAsync(Long pendingId, String email, String otp) {
        try {
            BrevoEmailService.EmailResult emailResult = brevoEmailService.sendOtpEmail(email, otp);
            if (pendingId != null) {
                PendingRegistration pr = pendingRegistrationRepository.findById(pendingId).orElse(null);
                if (pr != null) {
                    pr.setEmailSent(emailResult.success());
                    pendingRegistrationRepository.save(pr);
                }
            }
            if (emailResult.success()) {
                logger.info("OTP email sent successfully to: {}", email);
            } else {
                logger.error("Failed to send OTP email to {}: {}", email, emailResult.errorMessage());
            }
        } catch (Exception e) {
            logger.error("Async OTP email failed for {}: {}", email, e.getMessage(), e);
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

        if (password.length() > 72) {
            throw new IllegalArgumentException("Password cannot exceed 72 characters");
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
     * @param emailSent true if the OTP email was queued for delivery (the
     *                  email is sent asynchronously after the transaction
     *                  commits; actual delivery status is recorded on the
     *                  pending registration)
     * @param errorMessage error message if the email could not be queued, null otherwise
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
