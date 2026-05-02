package org.example.chat.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when input validation fails.
 */
public class ValidationException extends ChatApplicationException {
    
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST, cause);
    }
}
