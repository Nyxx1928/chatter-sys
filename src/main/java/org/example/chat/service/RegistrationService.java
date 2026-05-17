package org.example.chat.service;

import org.example.chat.entity.PendingRegistration;
import org.example.chat.entity.User;
import org.example.chat.repository.PendingRegistrationRepository;
import org.example.chat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for handling user registration with email verification.
 * Implements verify-first registration: email must be verified before account is created.
 */
@Service
public class RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BrevoEmailService brevoEmailService;
    private final ResendEmailService resendEmailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${brevo.enabled:false}")
    private boolean brevoEnabled;

    public RegistrationService(
            PendingRegistrationRepository pendingRegistrationRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            BrevoEmailService brevoEmailService,
            ResendEmailService resendEmailService) {
        this.pendingRegistrationRepository = pendingRegistrationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.brevoEmailService = brevoEmailService;
        this.resendEmailService = resendEmailService;
    }

    /**
     * Initiates user registration by creating a pending registration and sending verification email.
     * The user account is NOT created until email is verified.
     *
     * @param username the desired username
     * @param email the user's email address
     * @param password the plain text password
     * @param displayName the user's display name
     * @return RegistrationInitiationResult containing status and verification details
     * @throws IllegalArgumentException if validation fails or username/email already exists
     */
    @Transactional
    public RegistrationInitiationResult initiateRegistration(
            String username, String email, String password, String displayName) {
        
        logger.info("Initiating registration for username: {}, email: {}", username, email);

        // Validate input
        validateRegistrationInput(username, email, password, displayName);

        // Check if username already exists (in users or pending)
        if (userRepository.existsByUsername(username) || 
            pendingRegistrationRepository.existsByUsername(username)) {
            logger.warn("Registration failed: username already exists or pending: {}", username);
            throw new IllegalArgumentException("Username already exists");
        }

        // Check if email already exists (in users or pending)
        if (userRepository.existsByEmail(email) || 
            pendingRegistrationRepository.existsByEmail(email)) {
            logger.warn("Registration failed: email already exists or pending: {}", email);
            throw new IllegalArgumentException("Email already exists");
        }

        // Hash password
        String passwordHash = passwordEncoder.encode(password);

        // Create pending registration
        PendingRegistration pending = new PendingRegistration(
                username, email, passwordHash, displayName);

        // Send verification email (use Brevo if enabled, otherwise Resend)
        String verificationUrl = buildVerificationUrl(pending.getToken());
        boolean emailSent;
        String errorMessage;
        
        if (brevoEnabled) {
            BrevoEmailService.EmailResult emailResult = 
                    brevoEmailService.sendVerificationEmail(email, verificationUrl);
            emailSent = emailResult.success();
            errorMessage = emailResult.errorMessage();
        } else {
            ResendEmailService.EmailResult emailResult = 
                    resendEmailService.sendVerificationEmail(email, verificationUrl);
            emailSent = emailResult.success();
            errorMessage = emailResult.errorMessage();
        }

        pending.setEmailSent(emailSent);
        PendingRegistration savedPending = pendingRegistrationRepository.save(pending);

        logger.info("Pending registration created for username: {}, email sent: {}", 
                username, emailSent);

        return new RegistrationInitiationResult(
                savedPending.getToken(),
                verificationUrl,
                emailSent,
                errorMessage
        );
    }

    /**
     * Completes registration by verifying the token and creating the user account.
     *
     * @param token the verification token from the email
     * @return the newly created User
     * @throws IllegalArgumentException if token is invalid, expired, or already used
     */
    @Transactional
    public User completeRegistration(String token) {
        logger.info("Attempting to complete registration with token: {}", token);

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Verification token is required");
        }

        // Find pending registration
        PendingRegistration pending = pendingRegistrationRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        // Check if expired
        if (pending.isExpired()) {
            logger.warn("Verification token expired for username: {}", pending.getUsername());
            pendingRegistrationRepository.delete(pending);
            throw new IllegalArgumentException("Verification token has expired. Please register again.");
        }

        // Check if username or email was taken while pending
        if (userRepository.existsByUsername(pending.getUsername())) {
            logger.warn("Username taken during pending period: {}", pending.getUsername());
            pendingRegistrationRepository.delete(pending);
            throw new IllegalArgumentException("Username is no longer available");
        }

        if (userRepository.existsByEmail(pending.getEmail())) {
            logger.warn("Email taken during pending period: {}", pending.getEmail());
            pendingRegistrationRepository.delete(pending);
            throw new IllegalArgumentException("Email is no longer available");
        }

        // Create user account
        User user = new User();
        user.setUsername(pending.getUsername());
        user.setEmail(pending.getEmail());
        user.setPasswordHash(pending.getPasswordHash());
        user.setDisplayName(pending.getDisplayName());
        user.setCreatedAt(LocalDateTime.now());
        user.setOnline(false);
        user.setEmailVerified(true); // Already verified by clicking the link

        User savedUser = userRepository.save(user);

        // Delete pending registration
        pendingRegistrationRepository.delete(pending);

        logger.info("Registration completed successfully for username: {}", savedUser.getUsername());

        return savedUser;
    }

    /**
     * Resends verification email for a pending registration.
     *
     * @param email the email address of the pending registration
     * @throws IllegalArgumentException if no pending registration found
     */
    @Transactional
    public void resendVerificationEmail(String email) {
        logger.info("Resending verification email to: {}", email);

        PendingRegistration pending = pendingRegistrationRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No pending registration found for this email"));

        if (pending.isExpired()) {
            logger.warn("Pending registration expired for email: {}", email);
            pendingRegistrationRepository.delete(pending);
            throw new IllegalArgumentException("Registration expired. Please register again.");
        }

        String verificationUrl = buildVerificationUrl(pending.getToken());
        boolean emailSent;
        String errorMessage;
        
        if (brevoEnabled) {
            BrevoEmailService.EmailResult emailResult = 
                    brevoEmailService.sendVerificationEmail(email, verificationUrl);
            emailSent = emailResult.success();
            errorMessage = emailResult.errorMessage();
        } else {
            ResendEmailService.EmailResult emailResult = 
                    resendEmailService.sendVerificationEmail(email, verificationUrl);
            emailSent = emailResult.success();
            errorMessage = emailResult.errorMessage();
        }

        pending.setEmailSent(emailSent);
        pendingRegistrationRepository.save(pending);

        if (!emailSent) {
            logger.error("Failed to resend verification email to {}: {}", email, errorMessage);
            throw new IllegalArgumentException("Failed to send verification email");
        }

        logger.info("Verification email resent to: {}", email);
    }

    /**
     * Cleans up expired pending registrations.
     * Runs every hour.
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    @Transactional
    public void cleanupExpiredPendingRegistrations() {
        LocalDateTime cutoff = LocalDateTime.now();
        long count = pendingRegistrationRepository.findAll().stream()
                .filter(p -> p.getExpiryDate().isBefore(cutoff))
                .peek(pendingRegistrationRepository::delete)
                .count();

        if (count > 0) {
            logger.info("Cleaned up {} expired pending registrations", count);
        }
    }

    private String buildVerificationUrl(String token) {
        return baseUrl + "/api/auth/verify-email?token=" + token;
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

        // Basic email format validation
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
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
     * @param token the verification token
     * @param verificationUrl the verification URL
     * @param emailSent whether the email was sent successfully
     * @param errorMessage error message if email failed, null otherwise
     */
    public record RegistrationInitiationResult(
            String token,
            String verificationUrl,
            boolean emailSent,
            String errorMessage
    ) {}
}
