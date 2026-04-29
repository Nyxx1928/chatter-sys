package org.example.chat.service;

import org.example.chat.entity.User;
import org.example.chat.repository.UserRepository;
import org.example.chat.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for handling user authentication operations including registration and login.
 * Provides secure password hashing and JWT token generation.
 */
@Service
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthenticationService(UserRepository userRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user with the provided credentials.
     * Validates that username and email are unique, hashes the password, and persists the user.
     *
     * @param username the desired username
     * @param email the user's email address
     * @param password the plain text password
     * @param displayName the user's display name
     * @return the created User entity
     * @throws IllegalArgumentException if username or email already exists, or if input is invalid
     */
    @Transactional
    public User registerUser(String username, String email, String password, String displayName) {
        logger.info("Attempting to register user: {}", username);

        // Validate input
        validateRegistrationInput(username, email, password, displayName);

        // Check if username already exists
        if (userRepository.existsByUsername(username)) {
            logger.warn("Registration failed: username already exists: {}", username);
            throw new IllegalArgumentException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            logger.warn("Registration failed: email already exists: {}", email);
            throw new IllegalArgumentException("Email already exists");
        }

        // Hash password
        String passwordHash = passwordEncoder.encode(password);

        // Create user entity
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setDisplayName(displayName);
        user.setCreatedAt(LocalDateTime.now());
        user.setOnline(false);

        // Persist user
        User savedUser = userRepository.save(user);
        logger.info("Successfully registered user: {}", username);

        return savedUser;
    }

    /**
     * Authenticates a user with the provided credentials and returns a JWT token.
     *
     * @param username the username
     * @param password the plain text password
     * @return JWT token for the authenticated user
     * @throws IllegalArgumentException if credentials are invalid
     */
    public String authenticateUser(String username, String password) {
        logger.info("Attempting to authenticate user: {}", username);

        // Validate input
        if (username == null || username.trim().isEmpty()) {
            logger.warn("Authentication failed: username is empty");
            throw new IllegalArgumentException("Username cannot be empty");
        }

        if (password == null || password.isEmpty()) {
            logger.warn("Authentication failed: password is empty");
            throw new IllegalArgumentException("Password cannot be empty");
        }

        // Find user by username
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            logger.warn("Authentication failed: user not found: {}", username);
            throw new IllegalArgumentException("Invalid username or password");
        }

        User user = userOptional.get();

        // Verify password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            logger.warn("Authentication failed: invalid password for user: {}", username);
            throw new IllegalArgumentException("Invalid username or password");
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(username);
        logger.info("Successfully authenticated user: {}", username);

        return token;
    }

    /**
     * Retrieves a user by username.
     *
     * @param username the username to search for
     * @return the User entity
     * @throws IllegalArgumentException if user is not found
     */
    public User getUserByUsername(String username) {
        logger.debug("Retrieving user by username: {}", username);
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    /**
     * Updates a user's profile information.
     *
     * @param username the username of the user to update
     * @param email the new email (optional, null to keep current)
     * @param displayName the new display name (optional, null to keep current)
     * @return the updated User entity
     * @throws IllegalArgumentException if user is not found or email already exists
     */
    @Transactional
    public User updateUserProfile(String username, String email, String displayName) {
        logger.info("Updating profile for user: {}", username);

        User user = getUserByUsername(username);

        // Update email if provided
        if (email != null && !email.trim().isEmpty()) {
            // Check if email is already taken by another user
            if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                logger.warn("Profile update failed: email already exists: {}", email);
                throw new IllegalArgumentException("Email already exists");
            }
            user.setEmail(email);
        }

        // Update display name if provided
        if (displayName != null && !displayName.trim().isEmpty()) {
            user.setDisplayName(displayName);
        }

        User updatedUser = userRepository.save(user);
        logger.info("Successfully updated profile for user: {}", username);

        return updatedUser;
    }

    /**
     * Validates registration input parameters.
     *
     * @param username the username to validate
     * @param email the email to validate
     * @param password the password to validate
     * @param displayName the display name to validate
     * @throws IllegalArgumentException if any input is invalid
     */
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
}
