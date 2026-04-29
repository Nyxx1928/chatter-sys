package org.example.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.chat.entity.User;

/**
 * Response DTO for user login containing JWT token and user information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    
    private String token;
    private UserResponse user;
    
    /**
     * Creates a LoginResponse from a token and User entity.
     *
     * @param token the JWT token
     * @param user the User entity
     * @return LoginResponse instance
     */
    public static LoginResponse from(String token, User user) {
        return new LoginResponse(token, UserResponse.from(user));
    }
}
