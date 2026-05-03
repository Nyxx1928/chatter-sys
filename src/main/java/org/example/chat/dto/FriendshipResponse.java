package org.example.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for friendships.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendshipResponse {

    private PublicUserResponse friend;
    private LocalDateTime createdAt;
}
