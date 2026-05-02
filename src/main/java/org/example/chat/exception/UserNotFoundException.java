package org.example.chat.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested user is not found.
 */
public class UserNotFoundException extends ChatApplicationException {
    
    public UserNotFoundException(String message) {
        super(message, "USER_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
    
    public UserNotFoundException(Long userId) {
        super("User not found with id: " + userId, "USER_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
    
    public UserNotFoundException(String message, Throwable cause) {
        super(message, "USER_NOT_FOUND", HttpStatus.NOT_FOUND, cause);
    }
}
