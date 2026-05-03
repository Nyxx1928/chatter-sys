package org.example.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for listing pending friend requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestListResponse {

    private List<FriendRequestResponse> incoming;
    private List<FriendRequestResponse> outgoing;
}
