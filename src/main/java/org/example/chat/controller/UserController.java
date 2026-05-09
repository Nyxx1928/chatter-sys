package org.example.chat.controller;

import jakarta.validation.Valid;
import org.example.chat.dto.UpdateProfileRequest;
import org.example.chat.dto.UserResponse;
import org.example.chat.entity.User;
import org.example.chat.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user profile operations.
 * Handles retrieving and updating the current authenticated user's profile.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final AuthenticationService authenticationService;

    public UserController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Gets the current authenticated user's profile.
     *
     * @return ResponseEntity with UserResponse containing current user information
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        String username = getCurrentUsername();
        logger.info("Get current user request for: {}", username);

        User user = authenticationService.getUserByUsername(username);
        UserResponse response = UserResponse.from(user);

        return ResponseEntity.ok(response);
    }

    /**
     * Updates the current authenticated user's profile.
     *
     * @param request the update request containing optional email and display name
     * @return ResponseEntity with updated UserResponse
     */
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(@Valid @RequestBody UpdateProfileRequest request) {
        String username = getCurrentUsername();
        logger.info("Update profile request for user: {}", username);

        try {
            User updatedUser = authenticationService.updateUserProfile(
                username,
                request.getEmail(),
                request.getDisplayName()
            );

            UserResponse response = UserResponse.from(updatedUser);
            logger.info("Profile updated successfully for user: {}", username);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Profile update failed for user {}: {}", username, e.getMessage());
            throw e;
        }
    }

    /**
     * Permanently deletes the current authenticated user's account.
     * All messages, memberships, friendships, and friend requests are removed.
     * GROUP rooms the user created remain but lose their creator reference.
     *
     * @return 204 No Content on success
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount() {
        String username = getCurrentUsername();
        logger.info("Delete account request for: {}", username);

        authenticationService.deleteUser(username);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves the username of the current authenticated user from the security context.
     *
     * @return the username of the authenticated user
     * @throws IllegalStateException if no authenticated user is found
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.error("No authenticated user found in security context");
            throw new IllegalStateException("No authenticated user found");
        }

        return authentication.getName();
    }
}
