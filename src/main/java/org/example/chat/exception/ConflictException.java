package org.example.chat.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a request conflicts with existing data.
 */
public class ConflictException extends ChatApplicationException {

    public ConflictException(String message) {
        super(message, "CONFLICT", HttpStatus.CONFLICT);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, "CONFLICT", HttpStatus.CONFLICT, cause);
    }
}
