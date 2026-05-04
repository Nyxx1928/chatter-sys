package org.example.chat.controller;

import org.example.chat.dto.UserSearchResultResponse;
import org.example.chat.service.FriendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for user search.
 */
@RestController
@RequestMapping("/api/users")
public class UserSearchController {

    private static final Logger logger = LoggerFactory.getLogger(UserSearchController.class);

    private final FriendService friendService;

    public UserSearchController(FriendService friendService) {
        this.friendService = friendService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserSearchResultResponse>> searchUsers(
            @RequestParam(name = "q", required = false) String query,
            @AuthenticationPrincipal UserDetails userDetails) {
        logger.debug("User search by {} with query: {}", userDetails.getUsername(), query);

        List<UserSearchResultResponse> results = friendService.searchUsers(query, userDetails.getUsername());
        return ResponseEntity.ok(results);
    }
}
