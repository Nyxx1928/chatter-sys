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
    private final RegistrationService registrationService;

    public AuthenticationService(UserRepository userRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder,
                                 ChatRoomRepository chatRoomRepository,
                                 FriendshipRepository friendshipRepository,
                                 FriendRequestRepository friendRequestRepository,
                                 RegistrationService registrationService) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.chatRoomRepository = chatRoomRepository;
        this.friendshipRepository = friendshipRepository;
        this.friendRequestRepository = friendRequestRepository;
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
                result.emailSent(),
                result.errorMessage()
        );
    }

    public record RegistrationResult(
            boolean verificationEmailSent,
            String errorMessage
    ) {}

    /**
     * A dummy bcrypt hash used for timing-safe login.
     * When a user is not found, we hash against this dummy value so that
     * bcrypt evaluation always executes for the same duration, preventing
     * timing attacks that could reveal whether a username is registered.
     *
     * Generated at class-load time from a fixed dummy password.  This
     * guarantees the hash is always a syntactically valid bcrypt string
     * and never throws an exception inside BCryptPasswordEncoder.
     */
    private static final String DUMMY_HASH;
    static {
        DUMMY_HASH = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                .encode("dummy-timing-attack-prevention-value-4761");
    }

    /**
     * Authenticates a user with the provided credentials and returns a JWT token.
     *
     * Uses a timing-safe comparison approach (inspired by JLabs3/Laravel Sanctum):
     * always performs the password hash check, even when the user is not found,
     * to prevent timing-based email enumeration.
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

        // Find user by username — use a dummy hash if not found so that
        // passwordEncoder.matches() always executes (timing-safe).
        // This prevents attackers from distinguishing between "user not found"
        // and "wrong password" by measuring response time.
        Optional<User> userOptional = userRepository.findByUsername(username);
        String passwordHash = userOptional.map(User::getPasswordHash).orElse(DUMMY_HASH);

        // Verify password — ALWAYS executes, even for non-existent users
        if (!passwordEncoder.matches(password, passwordHash)) {
            logger.warn("Authentication failed: invalid password for user: {}", username);
            throw new IllegalArgumentException("Invalid username or password");
        }

        // Password matched — user must exist (the dummy hash can never match
        // because it's a bcrypt hash of a fixed dummy string, not the user's password).
        User user = userOptional.orElseThrow(() -> {
            logger.error("Timing-safe login invariant violated: password matched but user not found");
            return new IllegalStateException("Invalid username or password");
        });

        // Check email verification
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            logger.warn("Authentication failed: email not verified for user: {}", username);
            throw new IllegalArgumentException("Please verify your email before logging in");
        }

        // Only reached if user found AND password matched — generate JWT
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
        boolean emailChanged = false;
        if (email != null && !email.trim().isEmpty()) {
            // Check if email is already taken by another user
            if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                logger.warn("Profile update failed: email already exists: {}", email);
                throw new IllegalArgumentException("Email already exists");
            }
            if (!email.equals(user.getEmail())) {
                user.setEmail(email);
                user.setEmailVerified(false);
                emailChanged = true;
            }
        }

        // Update display name if provided
        if (displayName != null && !displayName.trim().isEmpty()) {
            user.setDisplayName(displayName);
        }

        User updatedUser = userRepository.save(user);

        if (emailChanged) {
            logger.info("Email changed for user: {}. Email verification required for new address.", username);
        }

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
        chatRoomRepository.findByCreatedById(user.getId())
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
