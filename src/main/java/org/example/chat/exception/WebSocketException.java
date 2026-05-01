package org.example.chat.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when WebSocket/STOMP operations fail.
 */
public class WebSocketException extends ChatApplicationException {
    
    public WebSocketException(String message) {
        super(message, "WEBSOCKET_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    public WebSocketException(String message, Throwable cause) {
        super(message, "WEBSOCKET_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
