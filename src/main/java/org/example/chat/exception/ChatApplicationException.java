package org.example.chat.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception class for all chat application exceptions.
 * Provides error code and HTTP status for consistent error handling.
 */
public class ChatApplicationException extends RuntimeException {
    
    private final String errorCode;
    private final HttpStatus httpStatus;
    
    public ChatApplicationException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public ChatApplicationException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
