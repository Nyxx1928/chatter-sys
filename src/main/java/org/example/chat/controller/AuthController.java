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
     *
     * @param request the registration request containing username, email, password, and display name
     * @return ResponseEntity with UserResponse and HTTP 201 Created status
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
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
            UserResponse response = UserResponse.from(result.user(), verificationUrl, result.verificationEmailSent());
            logger.info("User registered successfully: {}", request.getUsername());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Registration failed for username {}: {}", request.getUsername(), e.getMessage());
            throw e;
        }
    }

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
