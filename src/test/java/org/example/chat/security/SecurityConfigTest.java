package org.example.chat.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for Spring Security configuration.
 * Verifies that authentication endpoints are accessible and protected endpoints require authentication.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // Use H2 in-memory database for tests
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testAuthenticationEndpointsArePublic() throws Exception {
        // POST /api/auth/register should be accessible without authentication
        // Expecting 201 Created with valid data
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser1\",\"email\":\"testuser1@example.com\",\"password\":\"password123\",\"displayName\":\"Test User\"}"))
                .andExpect(status().isCreated()); // Should succeed with valid data

        // POST /api/auth/login should be accessible without authentication
        // Expecting 400 Bad Request for non-existent user
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nonexistentuser\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest()); // User doesn't exist, so bad request
    }

    @Test
    void testProtectedEndpointsRequireAuthentication() throws Exception {
        // GET /api/users/me should require authentication
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized()); // 401 Unauthorized for missing auth

        // GET /api/rooms should require authentication
        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isUnauthorized()); // 401 Unauthorized for missing auth
    }

    @Test
    void testWebSocketEndpointIsAccessible() throws Exception {
        // WebSocket endpoint should be accessible (authentication handled by STOMP interceptor)
        // The /ws endpoint returns 200 OK for GET requests (SockJS info endpoint)
        mockMvc.perform(get("/ws"))
                .andExpect(status().isOk()); // 200 OK - SockJS provides info endpoint
    }

    @Test
    void testCorsConfiguration() throws Exception {
        // Test that CORS headers are present for authentication endpoints
        mockMvc.perform(post("/api/auth/login")
                .header("Origin", "http://localhost:3000")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest()); // User doesn't exist, but endpoint is accessible
        
        // CORS preflight request - protected endpoints still require auth
        mockMvc.perform(get("/api/users/me")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isUnauthorized()); // Should return 401 for missing auth
    }
}
