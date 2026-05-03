package org.example.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for user search results.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResultResponse {

    private PublicUserResponse user;
    private RelationshipStatus relationshipStatus;
}
