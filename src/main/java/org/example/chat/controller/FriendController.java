package org.example.chat.controller;

import jakarta.validation.Valid;
import org.example.chat.dto.FriendRequestCreateRequest;
import org.example.chat.dto.FriendRequestListResponse;
import org.example.chat.dto.FriendRequestResponse;
import org.example.chat.dto.FriendshipResponse;
import org.example.chat.dto.PublicUserResponse;
import org.example.chat.service.FriendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for friend requests and friendships.
 */
@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private static final Logger logger = LoggerFactory.getLogger(FriendController.class);

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @PostMapping("/requests")
    public ResponseEntity<FriendRequestResponse> sendFriendRequest(
            @Valid @RequestBody FriendRequestCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        logger.info("Friend request send attempt by {}", userDetails.getUsername());

        FriendRequestResponse response = friendService.sendFriendRequest(
                userDetails.getUsername(),
                request.getRecipientId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/requests")
    public ResponseEntity<FriendRequestListResponse> listPendingRequests(
            @AuthenticationPrincipal UserDetails userDetails) {
        logger.debug("Listing friend requests for {}", userDetails.getUsername());

        FriendRequestListResponse response = friendService.listPendingRequests(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/requests/{id}/accept")
    public ResponseEntity<FriendshipResponse> acceptFriendRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        logger.info("Accepting friend request {} for {}", id, userDetails.getUsername());

        FriendshipResponse response = friendService.acceptFriendRequest(userDetails.getUsername(), id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/requests/{id}/decline")
    public ResponseEntity<Void> declineFriendRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        logger.info("Declining friend request {} for {}", id, userDetails.getUsername());

        friendService.declineFriendRequest(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<PublicUserResponse>> listFriends(
            @AuthenticationPrincipal UserDetails userDetails) {
        logger.debug("Listing friends for {}", userDetails.getUsername());

        List<PublicUserResponse> friends = friendService.listFriends(userDetails.getUsername());
        return ResponseEntity.ok(friends);
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> removeFriend(
            @PathVariable Long friendId,
            @AuthenticationPrincipal UserDetails userDetails) {
        logger.info("Remove friend request: user {} removing friend {}", userDetails.getUsername(), friendId);

        friendService.removeFriend(userDetails.getUsername(), friendId);
        return ResponseEntity.noContent().build();
    }
}
