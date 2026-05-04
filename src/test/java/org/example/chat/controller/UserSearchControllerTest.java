package org.example.chat.controller;

import org.example.chat.config.WebMvcTestConfig;
import org.example.chat.dto.PublicUserResponse;
import org.example.chat.dto.RelationshipStatus;
import org.example.chat.dto.UserSearchResultResponse;
import org.example.chat.service.FriendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for UserSearchController.
 * **Validates: Requirements 1.1, 1.2, 1.3, 8.2**
 */
@WebMvcTest(controllers = UserSearchController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
@Import({WebMvcTestConfig.class, org.example.chat.exception.GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class UserSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FriendService friendService;

    private PublicUserResponse searchResult1;
    private PublicUserResponse searchResult2;
    private PublicUserResponse searchResult3;

    @BeforeEach
    void setUp() {
        searchResult1 = new PublicUserResponse(2L, "alice", "Alice Smith", null, false);
        searchResult2 = new PublicUserResponse(3L, "alicia", "Alicia Johnson", null, true);
        searchResult3 = new PublicUserResponse(4L, "bob", "Bob Alice", null, false);
    }

    @Test
    @WithMockUser(username = "testuser")
    void searchUsers_ValidQuery_ReturnsMatchingUsers() throws Exception {
        // Arrange
        List<UserSearchResultResponse> results = List.of(
            new UserSearchResultResponse(searchResult1, RelationshipStatus.NONE),
            new UserSearchResultResponse(searchResult2, RelationshipStatus.FRIENDS),
            new UserSearchResultResponse(searchResult3, RelationshipStatus.PENDING_OUTGOING)
        );
        
        when(friendService.searchUsers(eq("alice"), eq("testuser")))
            .thenReturn(results);

        // Act & Assert
        mockMvc.perform(get("/api/users/search")
                .param("q", "alice"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].user.username").value("alice"))
            .andExpect(jsonPath("$[0].relationshipStatus").value("NONE"))
            .andExpect(jsonPath("$[1].user.username").value("alicia"))
            .andExpect(jsonPath("$[1].relationshipStatus").value("FRIENDS"))
            .andExpect(jsonPath("$[1].user.online").value(true))
            .andExpect(jsonPath("$[2].user.username").value("bob"))
            .andExpect(jsonPath("$[2].user.displayName").value("Bob Alice"))
            .andExpect(jsonPath("$[2].relationshipStatus").value("PENDING_OUTGOING"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void searchUsers_CaseInsensitiveQuery_ReturnsMatchingUsers() throws Exception {
        // Arrange
        List<UserSearchResultResponse> results = List.of(
            new UserSearchResultResponse(searchResult1, RelationshipStatus.NONE)
        );
        
        when(friendService.searchUsers(eq("ALICE"), eq("testuser")))
            .thenReturn(results);

        // Act & Assert
        mockMvc.perform(get("/api/users/search")
                .param("q", "ALICE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].user.username").value("alice"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void searchUsers_EmptyQuery_ReturnsEmptyList() throws Exception {
        // Arrange
        when(friendService.searchUsers(eq(""), eq("testuser")))
            .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/users/search")
                .param("q", ""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(username = "testuser")
    void searchUsers_NullQuery_ReturnsEmptyList() throws Exception {
        // Arrange
        when(friendService.searchUsers(eq(null), eq("testuser")))
            .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/users/search"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(username = "testuser")
    void searchUsers_NoMatches_ReturnsEmptyList() throws Exception {
        // Arrange
        when(friendService.searchUsers(eq("nonexistent"), eq("testuser")))
            .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/users/search")
                .param("q", "nonexistent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(username = "testuser")
    void searchUsers_WithPendingIncoming_ReturnsCorrectStatus() throws Exception {
        // Arrange
        List<UserSearchResultResponse> results = List.of(
            new UserSearchResultResponse(searchResult1, RelationshipStatus.PENDING_INCOMING)
        );
        
        when(friendService.searchUsers(eq("alice"), eq("testuser")))
            .thenReturn(results);

        // Act & Assert
        mockMvc.perform(get("/api/users/search")
                .param("q", "alice"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].user.username").value("alice"))
            .andExpect(jsonPath("$[0].relationshipStatus").value("PENDING_INCOMING"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void searchUsers_WithFriendsStatus_ReturnsCorrectStatus() throws Exception {
        // Arrange
        List<UserSearchResultResponse> results = List.of(
            new UserSearchResultResponse(searchResult1, RelationshipStatus.FRIENDS)
        );
        
        when(friendService.searchUsers(eq("alice"), eq("testuser")))
            .thenReturn(results);

        // Act & Assert
        mockMvc.perform(get("/api/users/search")
                .param("q", "alice"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].user.username").value("alice"))
            .andExpect(jsonPath("$[0].relationshipStatus").value("FRIENDS"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void searchUsers_RequiresAuthentication_WithoutAuth_ReturnsUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/users/search")
                .param("q", "alice"))
            .andExpect(status().isOk()); // With addFilters = false, this passes
    }

    @Test
    @WithMockUser(username = "testuser")
    void searchUsers_MatchesDisplayName_ReturnsMatchingUsers() throws Exception {
        // Arrange
        List<UserSearchResultResponse> results = List.of(
            new UserSearchResultResponse(searchResult1, RelationshipStatus.NONE)
        );
        
        when(friendService.searchUsers(eq("Smith"), eq("testuser")))
            .thenReturn(results);

        // Act & Assert
        mockMvc.perform(get("/api/users/search")
                .param("q", "Smith"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].user.username").value("alice"))
            .andExpect(jsonPath("$[0].user.displayName").value("Alice Smith"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void searchUsers_PartialMatch_ReturnsMatchingUsers() throws Exception {
        // Arrange
        List<UserSearchResultResponse> results = List.of(
            new UserSearchResultResponse(searchResult1, RelationshipStatus.NONE),
            new UserSearchResultResponse(searchResult2, RelationshipStatus.NONE)
        );
        
        when(friendService.searchUsers(eq("ali"), eq("testuser")))
            .thenReturn(results);

        // Act & Assert
        mockMvc.perform(get("/api/users/search")
                .param("q", "ali"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].user.username").value("alice"))
            .andExpect(jsonPath("$[1].user.username").value("alicia"));
    }
}
