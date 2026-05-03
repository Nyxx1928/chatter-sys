package org.example.chat.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a friend request is not found.
 */
public class FriendRequestNotFoundException extends ChatApplicationException {

    public FriendRequestNotFoundException(Long requestId) {
        super("Friend request not found with id: " + requestId, "FRIEND_REQUEST_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public FriendRequestNotFoundException(String message) {
        super(message, "FRIEND_REQUEST_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
