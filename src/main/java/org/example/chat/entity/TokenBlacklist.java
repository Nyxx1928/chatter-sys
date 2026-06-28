package org.example.chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Tracks revoked JWT tokens for multi-device session management.
 * When a user revokes other sessions, all their tokens (except the current one)
 * are added to this blacklist. The JwtAuthenticationFilter checks this table
 * before accepting any token.
 *
 * Inspired by JLabs3/Sanctum multi-device session management.
 */
@Entity
@Table(name = "token_blacklist", indexes = {
    @Index(name = "idx_token_jti", columnList = "tokenJti", unique = true),
    @Index(name = "idx_blacklist_expires", columnList = "expiresAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The JWT ID (jti claim) of the revoked token. */
    @Column(nullable = false, unique = true, length = 64)
    private String tokenJti;

    /** The username the token belonged to. */
    @Column(nullable = false, length = 50)
    private String username;

    /** When this blacklist entry expires (matches the token's expiry). */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public TokenBlacklist(String tokenJti, String username, LocalDateTime expiresAt) {
        this.tokenJti = tokenJti;
        this.username = username;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }
}
