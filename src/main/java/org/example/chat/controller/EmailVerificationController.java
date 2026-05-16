package org.example.chat.controller;

import jakarta.validation.Valid;
import org.example.chat.dto.ResendVerificationRequest;
import org.example.chat.entity.User;
import org.example.chat.service.EmailVerificationService;
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

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public EmailVerificationController(
            EmailVerificationService emailVerificationService,
            RegistrationService registrationService) {
        this.emailVerificationService = emailVerificationService;
        this.registrationService = registrationService;
    }

    /**
     * Verifies email and completes registration.
     * This endpoint handles both new registrations and existing user email changes.
     */
    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam("token") String token) {
        logger.info("Email verification request received");

        try {
            // Try to complete registration first (new user flow)
            try {
                User user = registrationService.completeRegistration(token);
                logger.info("Registration completed successfully for user: {}", user.getUsername());
                return redirectToFrontend("success", 
                        "Email verified successfully! You can now log in.");
            } catch (IllegalArgumentException e) {
                // If not a pending registration, try existing user verification
                emailVerificationService.verifyEmail(token);
                logger.info("Email verified successfully for existing user");
                return redirectToFrontend("success", "Email verified successfully");
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Email verification failed: {}", e.getMessage());
            return redirectToFrontend("error", e.getMessage());
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
