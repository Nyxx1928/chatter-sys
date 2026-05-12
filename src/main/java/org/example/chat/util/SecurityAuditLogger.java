package org.example.chat.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Component for logging security-related events for audit trail.
 * 
 * Logs all authorization failures, XSS attempts, and other security events
 * with sufficient context for security investigation.
 */
@Component
public class SecurityAuditLogger {

    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");
    private static final int MAX_LOG_FIELD_LENGTH = 500;

    /**
     * Logs an authorization failure when a user attempts to access a resource
     * they are not authorized for.
     *
     * @param userId the ID of the user who attempted unauthorized access
     * @param roomId the ID of the room they attempted to access
     * @param reason the reason for the authorization failure
     */
    public void logAuthorizationFailure(Long userId, Long roomId, String reason) {
        auditLogger.warn(
            "AUTHORIZATION_FAILURE: userId={}, roomId={}, reason={}, timestamp={}",
            userId, roomId, sanitizeField(reason), LocalDateTime.now()
        );
    }

    /**
     * Logs an XSS (Cross-Site Scripting) attempt when a user sends a message
     * containing dangerous HTML/JavaScript patterns.
     *
     * @param userId the ID of the user who attempted the XSS attack
     * @param roomId the ID of the room where the attempt occurred
     * @param content the malicious content that was detected
     */
    public void logXssAttempt(Long userId, Long roomId, String content) {
        auditLogger.warn(
            "XSS_ATTEMPT: userId={}, roomId={}, content={}, timestamp={}",
            userId, roomId, sanitizeField(content), LocalDateTime.now()
        );
    }

    /**
     * Logs a CSRF (Cross-Site Request Forgery) failure when a request lacks
     * a valid CSRF token.
     *
     * @param userId the ID of the user making the request (if available)
     * @param endpoint the endpoint that was targeted
     * @param reason the reason for the CSRF failure
     */
    public void logCsrfFailure(Long userId, String endpoint, String reason) {
        auditLogger.warn(
            "CSRF_FAILURE: userId={}, endpoint={}, reason={}, timestamp={}",
            userId, sanitizeField(endpoint), sanitizeField(reason), LocalDateTime.now()
        );
    }

    private String sanitizeField(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
        if (cleaned.length() > MAX_LOG_FIELD_LENGTH) {
            return cleaned.substring(0, MAX_LOG_FIELD_LENGTH) + "...";
        }
        return cleaned;
    }
}
