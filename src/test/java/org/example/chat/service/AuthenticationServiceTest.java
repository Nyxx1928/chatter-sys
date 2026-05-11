package org.example.chat.service;

import org.example.chat.entity.User;
import org.example.chat.repository.ChatRoomRepository;
import org.example.chat.repository.FriendRequestRepository;
import org.example.chat.repository.FriendshipRepository;
import org.example.chat.repository.UserRepository;
import org.example.chat.security.JwtUtil;
import org.example.chat.service.EmailVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthenticationService.
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private EmailVerificationService emailVerificationService;

    private AuthenticationService authenticationService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authenticationService = new AuthenticationService(
                userRepository, jwtUtil, passwordEncoder,
                chatRoomRepository, friendshipRepository, friendRequestRepository,
                emailVerificationService);
    }

    @Test
    void registerUser_ValidCredentials_Success() {
        // Arrange
        String username = "testuser";
        String email = "test@example.com";
        String password = "password123";
        String displayName = "Test User";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        // Act
        User result = authenticationService.registerUser(username, email, password, displayName);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(email, result.getEmail());
        assertEquals(displayName, result.getDisplayName());
        assertNotNull(result.getPasswordHash());
        assertNotEquals(password, result.getPasswordHash()); // Password should be hashed
        assertFalse(result.getOnline());
        assertFalse(result.getEmailVerified());
        assertNotNull(result.getCreatedAt());
        verify(emailVerificationService).createAndSendToken(result);

        verify(userRepository).existsByUsername(username);
        verify(userRepository).existsByEmail(email);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_DuplicateUsername_ThrowsException() {
        // Arrange
        String username = "existinguser";
        String email = "test@example.com";
        String password = "password123";
        String displayName = "Test User";

        when(userRepository.existsByUsername(username)).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authenticationService.registerUser(username, email, password, displayName)
        );

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository).existsByUsername(username);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_DuplicateEmail_ThrowsException() {
        // Arrange
        String username = "testuser";
        String email = "existing@example.com";
        String password = "password123";
        String displayName = "Test User";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authenticationService.registerUser(username, email, password, displayName)
        );

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository).existsByUsername(username);
        verify(userRepository).existsByEmail(email);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_EmptyUsername_ThrowsException() {
        // Arrange
        String username = "";
        String email = "test@example.com";
        String password = "password123";
        String displayName = "Test User";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authenticationService.registerUser(username, email, password, displayName)
        );

        assertEquals("Username cannot be empty", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_InvalidEmail_ThrowsException() {
        // Arrange
        String username = "testuser";
        String email = "invalid-email";
        String password = "password123";
        String displayName = "Test User";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authenticationService.registerUser(username, email, password, displayName)
        );

        assertEquals("Invalid email format", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_ShortPassword_ThrowsException() {
        // Arrange
        String username = "testuser";
        String email = "test@example.com";
        String password = "short";
        String displayName = "Test User";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authenticationService.registerUser(username, email, password, displayName)
        );

        assertEquals("Password must be at least 8 characters long", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void authenticateUser_ValidCredentials_ReturnsToken() {
        // Arrange
        String username = "testuser";
        String password = "password123";
        String hashedPassword = passwordEncoder.encode(password);
        String expectedToken = "jwt-token-123";

        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPasswordHash(hashedPassword);
        user.setEmailVerified(true);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(username)).thenReturn(expectedToken);
        when(emailVerificationService.isEmailVerified(user)).thenReturn(true);

        // Act
        String result = authenticationService.authenticateUser(username, password);

        // Assert
        assertEquals(expectedToken, result);
        verify(userRepository).findByUsername(username);
        verify(jwtUtil).generateToken(username);
    }

    @Test
    void authenticateUser_EmailNotVerified_ThrowsException() {
        // Arrange
        String username = "testuser";
        String password = "password123";
        String hashedPassword = passwordEncoder.encode(password);

        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPasswordHash(hashedPassword);
        user.setEmailVerified(false);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authenticationService.authenticateUser(username, password)
        );

        assertEquals("Please verify your email before logging in", exception.getMessage());
        verify(userRepository).findByUsername(username);
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void authenticateUser_InvalidUsername_ThrowsException() {
        // Arrange
        String username = "nonexistent";
        String password = "password123";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authenticationService.authenticateUser(username, password)
        );

        assertEquals("Invalid username or password", exception.getMessage());
        verify(userRepository).findByUsername(username);
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void authenticateUser_InvalidPassword_ThrowsException() {
        // Arrange
        String username = "testuser";
        String password = "wrongpassword";
        String correctPassword = "password123";
        String hashedPassword = passwordEncoder.encode(correctPassword);

        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPasswordHash(hashedPassword);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authenticationService.authenticateUser(username, password)
        );

        assertEquals("Invalid username or password", exception.getMessage());
        verify(userRepository).findByUsername(username);
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void authenticateUser_EmptyUsername_ThrowsException() {
        // Arrange
        String username = "";
        String password = "password123";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authenticationService.authenticateUser(username, password)
        );

        assertEquals("Username cannot be empty", exception.getMessage());
        verify(userRepository, never()).findByUsername(anyString());
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void authenticateUser_EmptyPassword_ThrowsException() {
        // Arrange
        String username = "testuser";
        String password = "";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authenticationService.authenticateUser(username, password)
        );

        assertEquals("Password cannot be empty", exception.getMessage());
        verify(userRepository, never()).findByUsername(anyString());
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void registerUser_UsernameTooLong_ThrowsException() {
        // Arrange
        String username = "a".repeat(51); // 51 characters
        String email = "test@example.com";
        String password = "password123";
        String displayName = "Test User";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authenticationService.registerUser(username, email, password, displayName)
        );

        assertEquals("Username cannot exceed 50 characters", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_EmailTooLong_ThrowsException() {
        // Arrange
        String username = "testuser";
        String email = "a".repeat(90) + "@example.com"; // Over 100 characters
        String password = "password123";
        String displayName = "Test User";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authenticationService.registerUser(username, email, password, displayName)
        );

        assertEquals("Email cannot exceed 100 characters", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_DisplayNameTooLong_ThrowsException() {
        // Arrange
        String username = "testuser";
        String email = "test@example.com";
        String password = "password123";
        String displayName = "a".repeat(101); // 101 characters

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authenticationService.registerUser(username, email, password, displayName)
        );

        assertEquals("Display name cannot exceed 100 characters", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
}
