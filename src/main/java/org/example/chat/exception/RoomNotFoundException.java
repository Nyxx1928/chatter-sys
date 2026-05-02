package org.example.chat.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested chat room is not found.
 */
public class RoomNotFoundException extends ChatApplicationException {
    
    public RoomNotFoundException(String message) {
        super(message, "ROOM_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
    
    public RoomNotFoundException(Long roomId) {
        super("Chat room not found with id: " + roomId, "ROOM_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
    
    public RoomNotFoundException(String message, Throwable cause) {
        super(message, "ROOM_NOT_FOUND", HttpStatus.NOT_FOUND, cause);
    }
}
