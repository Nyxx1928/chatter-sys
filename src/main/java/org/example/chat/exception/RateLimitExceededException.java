package org.example.chat.exception;

import org.springframework.http.HttpStatus;

public class RateLimitExceededException extends ChatApplicationException {

    public RateLimitExceededException(String message) {
        super(message, "RATE_LIMIT_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS);
    }
}
