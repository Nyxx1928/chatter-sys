package org.example.chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing a pending user registration awaiting OTP email verification.
 * This prevents creating user accounts before email is verified.
 */
@Entity
@Table(name = "pending_registrations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PendingRegistration {

    private static final int MAX_ATTEMPTS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false)
    private String otpHash;

    @Column(nullable = false)
    private LocalDateTime otpExpiry;

    @Column(nullable = false)
    private int attemptCount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Boolean emailSent = false;

    public PendingRegistration(String username, String email, String passwordHash, String displayName, String otpHash, LocalDateTime otpExpiry) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.otpHash = otpHash;
        this.otpExpiry = otpExpiry;
        this.attemptCount = 0;
        this.createdAt = LocalDateTime.now();
        this.emailSent = false;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(otpExpiry);
    }

    public boolean isMaxAttemptsExceeded() {
        return attemptCount >= MAX_ATTEMPTS;
    }

    public void incrementAttempts() {
        this.attemptCount++;
    }
}
