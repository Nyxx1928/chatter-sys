package org.example.chat.controller;

import jakarta.validation.Valid;
import org.example.chat.dto.LoginRequest;
import org.example.chat.dto.LoginResponse;
import org.example.chat.dto.RegisterRequest;
import org.example.chat.dto.UserResponse;
import org.example.chat.entity.User;
import org.example.chat.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations.
 * Handles user registration and login.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationService authenticationService;

    @Value("${app.verification.expose-link:false}")
    private boolean exposeVerificationLink;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Registers a new user.
     * Creates a pending registration and sends verification email.
     * User must verify email before account is created.
     *
     * @param request the registration request containing username, email, password, and display name
     * @return ResponseEntity with registration status and verification details
     */
    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegisterRequest request) {
        logger.info("Registration request received for username: {}", request.getUsername());

        try {
            AuthenticationService.RegistrationResult result = authenticationService.registerUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getDisplayName()
            );

            String verificationUrl = (exposeVerificationLink || !result.verificationEmailSent())
                    ? result.verificationUrl()
                    : null;

            RegistrationResponse response = new RegistrationResponse(
                    "Registration initiated. Please check your email to verify your account.",
                    result.verificationEmailSent(),
                    verificationUrl,
                    result.errorMessage()
            );

            logger.info("Registration initiated for username: {}, email sent: {}", 
                    request.getUsername(), result.verificationEmailSent());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Registration failed for username {}: {}", request.getUsername(), e.getMessage());
            throw e;
        }
    }

    /**
     * Response for registration initiation.
     */
    public record RegistrationResponse(
            String message,
            boolean emailSent,
            String verificationUrl,
            String errorMessage
    ) {}

    /**
     * Authenticates a user and returns a JWT token and CSRF token.
     *
     * @param request the login request containing username and password
     * @return ResponseEntity with LoginResponse containing token, CSRF token, and user information
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("Login request received for username: {}", request.getUsername());

        try {
            String token = authenticationService.authenticateUser(
                request.getUsername(),
                request.getPassword()
            );

            User user = authenticationService.getUserByUsername(request.getUsername());
            
            LoginResponse response = LoginResponse.from(token, user, null);

            logger.info("User logged in successfully: {}", request.getUsername());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Login failed for username {}: {}", request.getUsername(), e.getMessage());
            throw e;
        }
    }
}
