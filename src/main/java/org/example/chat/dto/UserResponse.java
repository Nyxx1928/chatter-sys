package org.example.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.chat.entity.User;

import java.time.LocalDateTime;

/**
 * Response DTO for User entity.
 * Excludes sensitive information like password hash.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    private Long id;
    private String username;
    private String email;
    private String displayName;
    private LocalDateTime createdAt;
    private LocalDateTime lastSeen;
    private Boolean online;
    private Boolean emailVerified;

    /**
     * Optional: returned by registration when enabled to help users verify without email delivery.
     */
    private String verificationUrl;

    /**
     * Optional: indicates whether an email was successfully handed off to SMTP.
     */
    private Boolean verificationEmailSent;
    
    /**
     * Creates a UserResponse from a User entity.
     *
     * @param user the User entity
     * @return UserResponse instance
     */
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getDisplayName(),
            user.getCreatedAt(),
            user.getLastSeen(),
            user.getOnline(),
            user.getEmailVerified(),
            null,
            null
        );
    }

    public static UserResponse from(User user, String verificationUrl, boolean verificationEmailSent) {
        UserResponse response = from(user);
        response.setVerificationUrl(verificationUrl);
        response.setVerificationEmailSent(verificationEmailSent);
        return response;
    }
}
