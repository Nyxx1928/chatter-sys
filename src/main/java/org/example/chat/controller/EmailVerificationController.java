package org.example.chat.controller;

import jakarta.validation.Valid;
import org.example.chat.dto.ResendVerificationRequest;
import org.example.chat.service.EmailVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class EmailVerificationController {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationController.class);

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam("token") String token) {
        logger.info("Email verification request received");

        try {
            emailVerificationService.verifyEmail(token);
            logger.info("Email verified successfully");
            return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
        } catch (IllegalArgumentException e) {
            logger.warn("Email verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        logger.info("Resend verification request for email: {}", request.getEmail());

        try {
            emailVerificationService.resendVerification(request.getEmail());
            logger.info("Verification email resent to: {}", request.getEmail());
            return ResponseEntity.ok(Map.of("message", "Verification email sent if the email is registered"));
        } catch (IllegalArgumentException e) {
            logger.warn("Resend verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
