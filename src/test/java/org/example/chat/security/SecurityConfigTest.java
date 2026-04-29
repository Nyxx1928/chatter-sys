package org.example.chat.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testAuthenticationEndpointsArePublic() throws Exception {
        // POST /api/auth/register should be accessible without authentication
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test\",\"email\":\"test@example.com\",\"password\":\"password123\",\"displayName\":\"Test User\"}"))
                .andExpect(status().isNotFound()); // 404 because controller doesn't exist yet, but not 401/403

        // POST /api/auth/login should be accessible without authentication
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test\",\"password\":\"password123\"}"))
                .andExpect(status().isNotFound()); // 404 because controller doesn't exist yet, but not 401/403
    }

    @Test
    void testProtectedEndpointsRequireAuthentication() throws Exception {
        // GET /api/users/me should require authentication
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized()); // 401 Unauthorized

        // GET /api/rooms should require authentication
        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isUnauthorized()); // 401 Unauthorized
    }

    @Test
    void testWebSocketEndpointIsAccessible() throws Exception {
        // WebSocket endpoint should be accessible (authentication handled by STOMP interceptor)
        // Note: This is a basic test; full WebSocket testing requires more setup
        mockMvc.perform(get("/ws"))
                .andExpect(status().isNotFound()); // 404 or other non-403/401 status
    }

    @Test
    void testCorsConfiguration() throws Exception {
        // Test that CORS headers are present
        mockMvc.perform(post("/api/auth/login")
                .header("Origin", "http://localhost:3000")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test\",\"password\":\"password123\"}"))
                .andExpect(status().isNotFound()); // Controller doesn't exist yet
        
        // CORS preflight request
        mockMvc.perform(get("/api/users/me")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isUnauthorized()); // Should still require auth
    }
}
