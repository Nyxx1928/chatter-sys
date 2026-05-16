package org.example.chat.service;

import org.example.chat.entity.User;
import org.example.chat.repository.ChatRoomRepository;
import org.example.chat.repository.FriendRequestRepository;
import org.example.chat.repository.FriendshipRepository;
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
    private final ChatRoomRepository chatRoomRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final EmailVerificationService emailVerificationService;
    private final RegistrationService registrationService;

    public AuthenticationService(UserRepository userRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder,
                                 ChatRoomRepository chatRoomRepository,
                                 FriendshipRepository friendshipRepository,
                                 FriendRequestRepository friendRequestRepository,
                                 EmailVerificationService emailVerificationService,
                                 RegistrationService registrationService) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.chatRoomRepository = chatRoomRepository;
        this.friendshipRepository = friendshipRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.emailVerificationService = emailVerificationService;
        this.registrationService = registrationService;
    }

    /**
     * Registers a new user with the provided credentials.
     * Creates a pending registration and sends verification email.
     * User account is NOT created until email is verified.
     *
     * @param username the desired username
     * @param email the user's email address
     * @param password the plain text password
     * @param displayName the user's display name
     * @return registration result including verification URL details
     * @throws IllegalArgumentException if username or email already exists, or if input is invalid
     */
    @Transactional
    public RegistrationResult registerUser(String username, String email, String password, String displayName) {
        logger.info("Attempting to register user: {}", username);

        RegistrationService.RegistrationInitiationResult result = 
                registrationService.initiateRegistration(username, email, password, displayName);

        logger.info("Registration initiated for user: {}, email sent: {}", username, result.emailSent());

        return new RegistrationResult(
                result.token(),
                result.verificationUrl(),
                result.emailSent(),
                result.errorMessage()
        );
    }

    public record RegistrationResult(
            String token,
            String verificationUrl,
            boolean verificationEmailSent,
            String errorMessage
    ) {}

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

        // Check email verification
        if (!emailVerificationService.isEmailVerified(user)) {
            logger.warn("Authentication failed: email not verified for user: {}", username);
            throw new IllegalArgumentException("Please verify your email before logging in");
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
            if (!email.equals(user.getEmail())) {
                user.setEmail(email);
                user.setEmailVerified(false);
                emailVerificationService.createAndSendToken(user);
            }
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
     * Permanently deletes the authenticated user's account.
     *
     * Deletion order:
     * 1. Null-out created_by_id on any GROUP rooms the user created (rooms stay, owner reference removed).
     * 2. Delete all friend requests involving the user.
     * 3. Delete all friendships involving the user.
     * 4. Delete the user — cascades remove their messages and room memberships automatically.
     *
     * @param username the username of the account to delete
     * @throws IllegalArgumentException if the user is not found
     */
    @Transactional
    public void deleteUser(String username) {
        logger.info("Deleting account for user: {}", username);

        User user = getUserByUsername(username);

        // 1. Null out created_by on GROUP rooms so they are not orphaned
        chatRoomRepository.findAll().stream()
                .filter(r -> r.getCreatedBy() != null && r.getCreatedBy().getId().equals(user.getId()))
                .forEach(r -> {
                    r.setCreatedBy(null);
                    chatRoomRepository.save(r);
                });

        // 2. Delete friend requests (sent or received)
        friendRequestRepository.deleteAll(
                friendRequestRepository.findByRequesterOrRecipient(user, user));

        // 3. Delete friendships
        friendshipRepository.deleteAll(
                friendshipRepository.findByUserAOrUserB(user, user));

        // 4. Delete user (cascades: messages, room_memberships)
        userRepository.delete(user);
        logger.info("Account deleted for user: {}", username);
    }

    /**
     * Validates registration input parameters.
     *
     * @param username the username to validate
     * @param email the email to validate
     * @param password the password to validate
     * @param displayName the display name to validate
     * @throws IllegalArgumentException if any input is invalid
     * @deprecated Use RegistrationService.initiateRegistration instead
     */
    @Deprecated
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
