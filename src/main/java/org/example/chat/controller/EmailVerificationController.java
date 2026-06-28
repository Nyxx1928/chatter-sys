package org.example.chat.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.chat.dto.ResendVerificationRequest;
import org.example.chat.entity.User;
import org.example.chat.service.EmailVerificationService;
import org.example.chat.service.RateLimiterService;
import org.example.chat.service.RegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for email verification operations.
 * Handles both new registration verification and existing user email verification.
 */
@RestController
@RequestMapping("/api/auth")
public class EmailVerificationController {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationController.class);

    private final EmailVerificationService emailVerificationService;
    private final RegistrationService registrationService;
    private final RateLimiterService rateLimiterService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public EmailVerificationController(
            EmailVerificationService emailVerificationService,
            RegistrationService registrationService,
            RateLimiterService rateLimiterService) {
        this.emailVerificationService = emailVerificationService;
        this.registrationService = registrationService;
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Verifies email and completes registration.
     * This endpoint handles both new registrations and existing user email changes.
     *
     * Flow:
     * 1. Try new user registration completion (PendingRegistration token)
     * 2. If that fails, try existing user email verification (VerificationToken token)
     * 3. If both fail, return the error from step 2
     *
     * This avoids a false-negative "error" when a user clicks a valid link twice —
     * the first click consumes the PendingRegistration, the second gracefully
     * falls through to verification (which may also fail since it's not a
     * VerificationToken), and the error message is informative.
     */
    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(
            @RequestParam("token") String token,
            HttpServletRequest httpRequest) {
        logger.info("Email verification request received");

        // Rate limit: 5 verification attempts per 1 minute per IP (JLabs3 pattern)
        rateLimiterService.checkEmailVerification(httpRequest.getRemoteAddr());

        // Step 1: Try new user registration completion (pending registration flow)
        Optional<User> newUser = tryCompleteRegistration(token);
        if (newUser.isPresent()) {
            logger.info("Registration completed successfully for user: {}", newUser.get().getUsername());
            return redirectToFrontend("success",
                    "Email verified successfully! You can now log in.");
        }

        // Step 2: Try existing user email verification (VerificationToken flow)
        try {
            emailVerificationService.verifyEmail(token);
            logger.info("Email verified successfully for existing user");
            return redirectToFrontend("success", "Email verified successfully");
        } catch (IllegalArgumentException e) {
            logger.warn("Email verification failed: {}", e.getMessage());
            return redirectToFrontend("error", e.getMessage());
        }
    }

    /**
     * Attempts to complete a new user registration with the given token.
     * Returns empty if the token does not correspond to a pending registration
     * (the token may be a VerificationToken, or already consumed).
     */
    private Optional<User> tryCompleteRegistration(String token) {
        try {
            return Optional.of(registrationService.completeRegistration(token));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Resends verification email.
     * Handles both pending registrations and existing users.
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        logger.info("Resend verification request for email: {}", request.getEmail());

        // Rate limit: 1 resend per 1 minute per email (JLabs3 pattern)
        rateLimiterService.checkResendVerification(request.getEmail());

        try {
            // Try pending registration first
            try {
                registrationService.resendVerificationEmail(request.getEmail());
                logger.info("Verification email resent to pending registration: {}", request.getEmail());
                return ResponseEntity.ok(Map.of("message", "Verification email sent"));
            } catch (IllegalArgumentException e) {
                // If not pending, try existing user
                emailVerificationService.resendVerification(request.getEmail());
                logger.info("Verification email resent to existing user: {}", request.getEmail());
                return ResponseEntity.ok(Map.of("message", "Verification email sent"));
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Resend verification failed: {}", e.getMessage());
            // Don't reveal whether email exists or not (security)
            return ResponseEntity.ok(Map.of("message", "If the email is registered, a verification email will be sent"));
        }
    }

    private ResponseEntity<Void> redirectToFrontend(String status, String message) {
        URI location = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/auth/verify-email")
                .queryParam("status", status)
                .queryParam("message", message)
                .build()
                .encode()
                .toUri();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, location.toString())
                .build();
    }
}
