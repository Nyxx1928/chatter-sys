package org.example.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.chat.entity.FriendRequest;

import java.time.LocalDateTime;

/**
 * Response DTO for friend requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestResponse {

    private Long id;
    private PublicUserResponse requester;
    private PublicUserResponse recipient;
    private LocalDateTime createdAt;

    public static FriendRequestResponse from(FriendRequest request) {
        return new FriendRequestResponse(
            request.getId(),
            PublicUserResponse.from(request.getRequester()),
            PublicUserResponse.from(request.getRecipient()),
            request.getCreatedAt()
        );
    }
}
