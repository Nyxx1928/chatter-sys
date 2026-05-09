package org.example.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.chat.config.WebMvcTestConfig;
import org.example.chat.dto.*;
import org.example.chat.exception.ConflictException;
import org.example.chat.exception.FriendRequestNotFoundException;
import org.example.chat.exception.ValidationException;
import org.example.chat.service.FriendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for FriendController.
 * **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 8.1**
 */
@WebMvcTest(controllers = FriendController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
@Import({WebMvcTestConfig.class, org.example.chat.exception.GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class FriendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FriendService friendService;

    private PublicUserResponse testUser;
    private PublicUserResponse friendUser;
    private FriendRequestResponse testRequest;

    @BeforeEach
    void setUp() {
        testUser = new PublicUserResponse(1L, "testuser", "Test User", null, false);
        friendUser = new PublicUserResponse(2L, "frienduser", "Friend User", null, false);
        
        testRequest = new FriendRequestResponse(
            1L,
            testUser,
            friendUser,
            LocalDateTime.now()
        );
    }

    @Test
    @WithMockUser(username = "testuser")
    void sendFriendRequest_ValidRequest_ReturnsCreated() throws Exception {
        // Arrange
        FriendRequestCreateRequest request = new FriendRequestCreateRequest(2L);
        
        when(friendService.sendFriendRequest(eq("testuser"), eq(2L)))
            .thenReturn(testRequest);

        // Act & Assert
        mockMvc.perform(post("/api/friends/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.requester.username").value("testuser"))
            .andExpect(jsonPath("$.recipient.username").value("frienduser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void sendFriendRequest_SelfRequest_ReturnsBadRequest() throws Exception {
        // Arrange
        FriendRequestCreateRequest request = new FriendRequestCreateRequest(1L);
        
        when(friendService.sendFriendRequest(eq("testuser"), eq(1L)))
            .thenThrow(new ValidationException("Cannot send friend request to yourself"));

        // Act & Assert
        mockMvc.perform(post("/api/friends/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Cannot send friend request to yourself"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void sendFriendRequest_DuplicateRequest_ReturnsConflict() throws Exception {
        // Arrange
        FriendRequestCreateRequest request = new FriendRequestCreateRequest(2L);
        
        when(friendService.sendFriendRequest(eq("testuser"), eq(2L)))
            .thenThrow(new ConflictException("Friend request already sent"));

        // Act & Assert
        mockMvc.perform(post("/api/friends/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Friend request already sent"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void sendFriendRequest_AlreadyFriends_ReturnsConflict() throws Exception {
        // Arrange
        FriendRequestCreateRequest request = new FriendRequestCreateRequest(2L);
        
        when(friendService.sendFriendRequest(eq("testuser"), eq(2L)))
            .thenThrow(new ConflictException("You are already friends with this user"));

        // Act & Assert
        mockMvc.perform(post("/api/friends/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("You are already friends with this user"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void sendFriendRequest_NullRecipientId_ReturnsBadRequest() throws Exception {
        // Arrange
        String requestJson = "{}";

        // Act & Assert
        mockMvc.perform(post("/api/friends/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void listPendingRequests_ReturnsIncomingAndOutgoing() throws Exception {
        // Arrange
        FriendRequestResponse incoming = new FriendRequestResponse(
            1L, friendUser, testUser, LocalDateTime.now()
        );
        FriendRequestResponse outgoing = new FriendRequestResponse(
            2L, testUser, friendUser, LocalDateTime.now()
        );
        
        FriendRequestListResponse response = new FriendRequestListResponse(
            List.of(incoming),
            List.of(outgoing)
        );
        
        when(friendService.listPendingRequests(eq("testuser")))
            .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/friends/requests"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.incoming").isArray())
            .andExpect(jsonPath("$.incoming[0].id").value(1))
            .andExpect(jsonPath("$.incoming[0].requester.username").value("frienduser"))
            .andExpect(jsonPath("$.outgoing").isArray())
            .andExpect(jsonPath("$.outgoing[0].id").value(2))
            .andExpect(jsonPath("$.outgoing[0].recipient.username").value("frienduser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void listPendingRequests_EmptyLists_ReturnsEmptyArrays() throws Exception {
        // Arrange
        FriendRequestListResponse response = new FriendRequestListResponse(
            List.of(),
            List.of()
        );
        
        when(friendService.listPendingRequests(eq("testuser")))
            .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/friends/requests"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.incoming").isArray())
            .andExpect(jsonPath("$.incoming").isEmpty())
            .andExpect(jsonPath("$.outgoing").isArray())
            .andExpect(jsonPath("$.outgoing").isEmpty());
    }

    @Test
    @WithMockUser(username = "testuser")
    void acceptFriendRequest_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        FriendshipResponse response = new FriendshipResponse(
            friendUser,
            LocalDateTime.now(),
            42L
        );
        
        when(friendService.acceptFriendRequest(eq("testuser"), eq(1L)))
            .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/friends/requests/1/accept"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.friend.username").value("frienduser"))
            .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @WithMockUser(username = "testuser")
    void acceptFriendRequest_NotFound_ReturnsNotFound() throws Exception {
        // Arrange
        when(friendService.acceptFriendRequest(eq("testuser"), eq(999L)))
            .thenThrow(new FriendRequestNotFoundException(999L));

        // Act & Assert
        mockMvc.perform(post("/api/friends/requests/999/accept"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Friend request not found with id: 999"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void declineFriendRequest_ValidRequest_ReturnsNoContent() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/friends/requests/1/decline"))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "testuser")
    void declineFriendRequest_NotFound_ReturnsNotFound() throws Exception {
        // Arrange
        doThrow(new FriendRequestNotFoundException(999L))
            .when(friendService).declineFriendRequest(eq("testuser"), eq(999L));

        // Act & Assert
        mockMvc.perform(post("/api/friends/requests/999/decline"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Friend request not found with id: 999"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void listFriends_ReturnsFriendsList() throws Exception {
        // Arrange
        List<PublicUserResponse> friends = List.of(
            friendUser,
            new PublicUserResponse(3L, "friend2", "Friend Two", null, true)
        );
        
        when(friendService.listFriends(eq("testuser")))
            .thenReturn(friends);

        // Act & Assert
        mockMvc.perform(get("/api/friends"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].username").value("frienduser"))
            .andExpect(jsonPath("$[1].username").value("friend2"))
            .andExpect(jsonPath("$[1].online").value(true));
    }

    @Test
    @WithMockUser(username = "testuser")
    void listFriends_EmptyList_ReturnsEmptyArray() throws Exception {
        // Arrange
        when(friendService.listFriends(eq("testuser")))
            .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/friends"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }
}
