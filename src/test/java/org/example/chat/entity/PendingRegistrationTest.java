package org.example.chat.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PendingRegistrationTest {

    @Test
    void isExpired_WhenOtpExpiryInPast_ReturnsTrue() {
        PendingRegistration pending = new PendingRegistration();
        pending.setOtpExpiry(LocalDateTime.now().minusMinutes(1));
        assertTrue(pending.isExpired());
    }

    @Test
    void isExpired_WhenOtpExpiryInFuture_ReturnsFalse() {
        PendingRegistration pending = new PendingRegistration();
        pending.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        assertFalse(pending.isExpired());
    }

    @Test
    void isMaxAttemptsExceeded_WhenAttemptCountIs3_ReturnsTrue() {
        PendingRegistration pending = new PendingRegistration();
        pending.setAttemptCount(3);
        assertTrue(pending.isMaxAttemptsExceeded());
    }

    @Test
    void isMaxAttemptsExceeded_WhenAttemptCountIs0_ReturnsFalse() {
        PendingRegistration pending = new PendingRegistration();
        pending.setAttemptCount(0);
        assertFalse(pending.isMaxAttemptsExceeded());
    }

    @Test
    void isMaxAttemptsExceeded_WhenAttemptCountIs2_ReturnsFalse() {
        PendingRegistration pending = new PendingRegistration();
        pending.setAttemptCount(2);
        assertFalse(pending.isMaxAttemptsExceeded());
    }

    @Test
    void incrementAttempts_IncrementsByOne() {
        PendingRegistration pending = new PendingRegistration();
        pending.setAttemptCount(0);
        pending.incrementAttempts();
        assertEquals(1, pending.getAttemptCount());
    }

    @Test
    void incrementAttempts_MultipleCalls_IncrementsCorrectly() {
        PendingRegistration pending = new PendingRegistration();
        pending.setAttemptCount(0);
        pending.incrementAttempts();
        pending.incrementAttempts();
        pending.incrementAttempts();
        assertEquals(3, pending.getAttemptCount());
    }

    @Test
    void isMaxAttemptsExceeded_AfterThreeIncrements_ReturnsTrue() {
        PendingRegistration pending = new PendingRegistration();
        pending.setAttemptCount(0);
        pending.incrementAttempts();
        pending.incrementAttempts();
        pending.incrementAttempts();
        assertTrue(pending.isMaxAttemptsExceeded());
    }

    @Test
    void constructor_SetsFieldsCorrectly() {
        String username = "testuser";
        String email = "test@example.com";
        String passwordHash = "$2a$10$hash";
        String displayName = "Test User";
        String otpHash = "$2a$10$otphash";
        LocalDateTime otpExpiry = LocalDateTime.now().plusMinutes(10);

        PendingRegistration pending = new PendingRegistration(
                username, email, passwordHash, displayName, otpHash, otpExpiry);

        assertEquals(username, pending.getUsername());
        assertEquals(email, pending.getEmail());
        assertEquals(passwordHash, pending.getPasswordHash());
        assertEquals(displayName, pending.getDisplayName());
        assertEquals(otpHash, pending.getOtpHash());
        assertEquals(otpExpiry, pending.getOtpExpiry());
        assertEquals(0, pending.getAttemptCount());
        assertFalse(pending.getEmailSent());
        assertNotNull(pending.getCreatedAt());
    }
}
