package org.example.chat.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

/**
 * Utility class for JWT token generation and validation.
 * Provides methods to create, validate, and extract information from JWT tokens.
 */
@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * Generates a JWT token for the given username.
     * The token includes the username as the subject, a unique JWT ID (jti),
     * issued-at, and expiration claims.
     *
     * @param username the username to include in the token
     * @return the generated JWT token as a string
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        SecretKey key = getSigningKey();

        String token = Jwts.builder()
                .id(UUID.randomUUID().toString())  // unique jti for revocation
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();

        logger.debug("Generated JWT token for user: {}", username);
        return token;
    }

    /**
     * Extracts the JWT ID (jti) from a token.
     * Used for token revocation/blacklisting.
     *
     * @param token the JWT token
     * @return the JWT ID
     */
    public String getTokenId(String token) {
        SecretKey key = getSigningKey();
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getId();
    }

    /**
     * Extracts the issued-at timestamp from a token and converts it to
     * a LocalDateTime (using the system default time zone).
     * Used for batch revocation checks in session management.
     *
     * @param token the JWT token
     * @return the issued-at time as LocalDateTime
     */
    public LocalDateTime getIssuedAt(String token) {
        SecretKey key = getSigningKey();
        Date issuedAt = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getIssuedAt();
        return issuedAt.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();
    }

    /**
     * Validates a JWT token by verifying its signature and expiration.
     *
     * @param token the JWT token to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = getSigningKey();
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            logger.debug("JWT token validated successfully");
            return true;
        } catch (SignatureException ex) {
            logger.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Extracts the username from a JWT token.
     *
     * @param token the JWT token to extract the username from
     * @return the username contained in the token
     * @throws ExpiredJwtException if the token has expired
     * @throws MalformedJwtException if the token is malformed
     */
    public String getUsernameFromToken(String token) {
        SecretKey key = getSigningKey();
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String username = claims.getSubject();
        logger.debug("Extracted username from JWT token: {}", username);
        return username;
    }

    /**
     * Creates a signing key from the configured secret.
     *
     * @return the SecretKey for signing and verifying JWT tokens
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
