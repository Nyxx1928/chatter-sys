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
            user.getOnline()
        );
    }
}
