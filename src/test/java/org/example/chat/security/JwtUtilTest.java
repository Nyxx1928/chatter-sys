package org.example.chat.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtUtil class.
 * Tests token generation, validation, and username extraction.
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String TEST_SECRET = "test-secret-key-for-jwt-token-generation-minimum-256-bits-required";
    private static final long TEST_EXPIRATION = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", TEST_EXPIRATION);
    }

    @Test
    void generateToken_ValidUsername_ReturnsToken() {
        // Arrange
        String username = "testuser";

        // Act
        String token = jwtUtil.generateToken(username);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts: header.payload.signature
    }

    @Test
    void generateToken_DifferentUsernames_ReturnsDifferentTokens() {
        // Arrange
        String username1 = "user1";
        String username2 = "user2";

        // Act
        String token1 = jwtUtil.generateToken(username1);
        String token2 = jwtUtil.generateToken(username2);

        // Assert
        assertNotEquals(token1, token2);
    }

    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        // Arrange
        String username = "testuser";
        String token = jwtUtil.generateToken(username);

        // Act
        boolean isValid = jwtUtil.validateToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void validateToken_InvalidSignature_ReturnsFalse() {
        // Arrange
        String username = "testuser";
        String token = jwtUtil.generateToken(username);
        // Tamper with the token by changing the last character
        String tamperedToken = token.substring(0, token.length() - 1) + "X";

        // Act
        boolean isValid = jwtUtil.validateToken(tamperedToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void validateToken_MalformedToken_ReturnsFalse() {
        // Arrange
        String malformedToken = "not.a.valid.jwt.token";

        // Act
        boolean isValid = jwtUtil.validateToken(malformedToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void validateToken_ExpiredToken_ReturnsFalse() {
        // Arrange
        String username = "testuser";
        Date now = new Date();
        Date pastExpiry = new Date(now.getTime() - 1000); // Expired 1 second ago

        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject(username)
                .issuedAt(new Date(now.getTime() - 2000))
                .expiration(pastExpiry)
                .signWith(key)
                .compact();

        // Act
        boolean isValid = jwtUtil.validateToken(expiredToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void validateToken_EmptyToken_ReturnsFalse() {
        // Arrange
        String emptyToken = "";

        // Act
        boolean isValid = jwtUtil.validateToken(emptyToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void validateToken_NullToken_ReturnsFalse() {
        // Arrange
        String nullToken = null;

        // Act
        boolean isValid = jwtUtil.validateToken(nullToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void getUsernameFromToken_ValidToken_ReturnsUsername() {
        // Arrange
        String expectedUsername = "testuser";
        String token = jwtUtil.generateToken(expectedUsername);

        // Act
        String actualUsername = jwtUtil.getUsernameFromToken(token);

        // Assert
        assertEquals(expectedUsername, actualUsername);
    }

    @Test
    void getUsernameFromToken_DifferentUsernames_ReturnsCorrectUsername() {
        // Arrange
        String username1 = "user1";
        String username2 = "user2";
        String token1 = jwtUtil.generateToken(username1);
        String token2 = jwtUtil.generateToken(username2);

        // Act
        String extractedUsername1 = jwtUtil.getUsernameFromToken(token1);
        String extractedUsername2 = jwtUtil.getUsernameFromToken(token2);

        // Assert
        assertEquals(username1, extractedUsername1);
        assertEquals(username2, extractedUsername2);
    }

    @Test
    void getUsernameFromToken_ExpiredToken_ThrowsExpiredJwtException() {
        // Arrange
        String username = "testuser";
        Date now = new Date();
        Date pastExpiry = new Date(now.getTime() - 1000); // Expired 1 second ago

        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject(username)
                .issuedAt(new Date(now.getTime() - 2000))
                .expiration(pastExpiry)
                .signWith(key)
                .compact();

        // Act & Assert
        assertThrows(ExpiredJwtException.class, () -> {
            jwtUtil.getUsernameFromToken(expiredToken);
        });
    }

    @Test
    void getUsernameFromToken_MalformedToken_ThrowsException() {
        // Arrange
        String malformedToken = "not.a.valid.jwt";

        // Act & Assert
        assertThrows(Exception.class, () -> {
            jwtUtil.getUsernameFromToken(malformedToken);
        });
    }

    @Test
    void tokenLifecycle_GenerateValidateExtract_WorksCorrectly() {
        // Arrange
        String username = "lifecycletest";

        // Act
        String token = jwtUtil.generateToken(username);
        boolean isValid = jwtUtil.validateToken(token);
        String extractedUsername = jwtUtil.getUsernameFromToken(token);

        // Assert
        assertNotNull(token);
        assertTrue(isValid);
        assertEquals(username, extractedUsername);
    }
}
