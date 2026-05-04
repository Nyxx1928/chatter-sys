package org.example.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.chat.entity.User;

import java.time.LocalDateTime;

/**
 * Public-facing user response without sensitive fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicUserResponse {

    private Long id;
    private String username;
    private String displayName;
    private LocalDateTime lastSeen;
    private Boolean online;

    public static PublicUserResponse from(User user) {
        return new PublicUserResponse(
            user.getId(),
            user.getUsername(),
            user.getDisplayName(),
            user.getLastSeen(),
            user.getOnline()
        );
    }
}
