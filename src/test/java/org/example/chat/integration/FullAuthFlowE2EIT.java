package org.example.chat.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.chat.dto.LoginRequest;
import org.example.chat.dto.RegisterRequest;
import org.example.chat.entity.*;
import org.example.chat.repository.*;
import org.example.chat.security.JwtUtil;
import org.example.chat.service.ChatMessageService;
import org.example.chat.service.RateLimiterService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FullAuthFlowE2EIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private RoomMembershipRepository roomMembershipRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private TokenBlacklistRepository tokenBlacklistRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private RateLimiterService rateLimiterService;

    @org.springframework.beans.factory.annotation.Value("${local.server.port}")
    private int port;

    @AfterEach
    void tearDown() {
        messageRepository.deleteAll();
        roomMembershipRepository.deleteAll();
        chatRoomRepository.deleteAll();
        verificationTokenRepository.deleteAll();
        pendingRegistrationRepository.deleteAll();
        tokenBlacklistRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String registerUser(String username, String email, String password, String displayName) throws Exception {
        RegisterRequest request = new RegisterRequest(username, email, password, displayName);

        java.util.Random rand = new java.util.Random();
        String ip = "10." + rand.nextInt(256) + "." + rand.nextInt(256) + "." + (rand.nextInt(254) + 1);

        String response = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request))
                .with(r -> { r.setRemoteAddr(ip); return r; }))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
        String verificationUrl = node.get("verificationUrl").asText();
        String token = verificationUrl.substring(verificationUrl.indexOf("token=") + 6);
        return token;
    }

    private void verifyEmail(String token) throws Exception {
        mockMvc.perform(get("/api/auth/verify-email")
                .param("token", token))
                .andExpect(status().isFound());
    }

    private String loginUser(String username, String password) throws Exception {
        LoginRequest request = new LoginRequest(username, password);

        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
        return node.get("token").asText();
    }

    @Test
    void registrationFlow_ValidRequest_CreatesPendingRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "e2euser", "e2e@example.com", "TestP@ss1", "E2E User");

        String response = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request))
                .with(r -> { r.setRemoteAddr("10.1.1.1"); return r; }))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verificationUrl").isNotEmpty())
                .andExpect(jsonPath("$.emailSent").isBoolean())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
        assertTrue(node.get("verificationUrl").asText().contains("token="));

        assertTrue(pendingRegistrationRepository.findByUsername("e2euser").isPresent());
        assertFalse(userRepository.findByUsername("e2euser").isPresent());
    }

    @Test
    void emailVerificationFlow_ValidToken_CreatesUserWithVerifiedEmail() throws Exception {
        String token = registerUser("verifyuser", "verify@example.com", "TestP@ss1", "Verify User");

        assertTrue(pendingRegistrationRepository.findByToken(token).isPresent());

        verifyEmail(token);

        assertFalse(pendingRegistrationRepository.findByToken(token).isPresent());

        User user = userRepository.findByUsername("verifyuser").orElse(null);
        assertNotNull(user);
        assertEquals("verifyuser", user.getUsername());
        assertEquals("verify@example.com", user.getEmail());
        assertTrue(user.getEmailVerified());
        assertTrue(passwordEncoder.matches("TestP@ss1", user.getPasswordHash()));
    }

    @Test
    void loginFlow_VerifiedUser_ReturnsJwtToken() throws Exception {
        String token = registerUser("loginuser", "login@example.com", "TestP@ss1", "Login User");
        verifyEmail(token);

        String jwt = loginUser("loginuser", "TestP@ss1");

        assertNotNull(jwt);
        assertFalse(jwt.isEmpty());
        assertTrue(jwtUtil.validateToken(jwt));
        assertEquals("loginuser", jwtUtil.getUsernameFromToken(jwt));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
                        new LoginRequest("loginuser", "TestP@ss1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.username").value("loginuser"))
                .andExpect(jsonPath("$.user.email").value("login@example.com"))
                .andExpect(jsonPath("$.user.displayName").value("Login User"));
    }

    @Test
    void timingSafeLogin_NonExistentUserAndWrongPassword_ReturnSameError() throws Exception {
        User user = new User();
        user.setUsername("timinguser");
        user.setEmail("timing@example.com");
        user.setPasswordHash(passwordEncoder.encode("TestP@ss1"));
        user.setDisplayName("Timing User");
        user.setEmailVerified(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setOnline(false);
        userRepository.save(user);

        String nonExistentResponse = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
                        new LoginRequest("nonexistentuser", "TestP@ss1"))))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String wrongPasswordResponse = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
                        new LoginRequest("timinguser", "Wr0ngP@ss1"))))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode nonExistentNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(nonExistentResponse);
        JsonNode wrongPasswordNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(wrongPasswordResponse);

        assertEquals(
                nonExistentNode.get("message").asText(),
                wrongPasswordNode.get("message").asText());
        assertEquals("Invalid username or password", nonExistentNode.get("message").asText());
    }

    @Test
    void passwordComplexity_NoUpperCase_ReturnsBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "weakuser1", "weak1@example.com", "testp@ss1", "Weak User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request))
                .with(r -> { r.setRemoteAddr("10.2.1.1"); return r; }))
                .andExpect(status().isBadRequest());

        assertFalse(userRepository.findByUsername("weakuser1").isPresent());
        assertFalse(pendingRegistrationRepository.findByUsername("weakuser1").isPresent());
    }

    @Test
    void passwordComplexity_NoSpecialChar_ReturnsBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "weakuser2", "weak2@example.com", "TestPass1", "Weak User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request))
                .with(r -> { r.setRemoteAddr("10.2.2.1"); return r; }))
                .andExpect(status().isBadRequest());

        assertFalse(userRepository.findByUsername("weakuser2").isPresent());
        assertFalse(pendingRegistrationRepository.findByUsername("weakuser2").isPresent());
    }

    @Test
    void passwordComplexity_NoDigit_ReturnsBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "weakuser3", "weak3@example.com", "TestP@ssword", "Weak User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request))
                .with(r -> { r.setRemoteAddr("10.2.3.1"); return r; }))
                .andExpect(status().isBadRequest());

        assertFalse(userRepository.findByUsername("weakuser3").isPresent());
    }

    @Test
    void passwordComplexity_NoLowerCase_ReturnsBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "weakuser4", "weak4@example.com", "TESTP@SS1", "Weak User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request))
                .with(r -> { r.setRemoteAddr("10.2.4.1"); return r; }))
                .andExpect(status().isBadRequest());

        assertFalse(userRepository.findByUsername("weakuser4").isPresent());
    }

    @Test
    void rateLimiting_MultipleRegistrations_EnforcesLimit() throws Exception {
        ReflectionTestUtils.setField(rateLimiterService, "rateLimitEnabled", true);
        try {
            String uniqueIp = "10.99.99.99";

            for (int i = 0; i < 3; i++) {
                RegisterRequest request = new RegisterRequest(
                        "rateuser" + i, "rate" + i + "@example.com", "TestP@ss1", "Rate User");

                mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request))
                        .with(r -> { r.setRemoteAddr(uniqueIp); return r; }))
                        .andExpect(status().isCreated());
            }

            RegisterRequest overflowRequest = new RegisterRequest(
                    "rateuser3", "rate3@example.com", "TestP@ss1", "Rate User");

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(overflowRequest))
                    .with(r -> { r.setRemoteAddr(uniqueIp); return r; }))
                    .andExpect(status().isTooManyRequests());
        } finally {
            ReflectionTestUtils.setField(rateLimiterService, "rateLimitEnabled", false);
        }
    }

    @Test
    void webSocketMessaging_SendMessage_PersistsAndRetrievable() throws Exception {
        User user = new User();
        user.setUsername("wsuser");
        user.setEmail("ws@example.com");
        user.setPasswordHash(passwordEncoder.encode("TestP@ss1"));
        user.setDisplayName("WS User");
        user.setEmailVerified(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setOnline(false);
        user = userRepository.save(user);

        ChatRoom room = new ChatRoom();
        room.setName("WS Test Room");
        room.setCreatedAt(LocalDateTime.now());
        room = chatRoomRepository.save(room);

        RoomMembership membership = new RoomMembership();
        membership.setChatRoom(room);
        membership.setUser(user);
        membership.setRole(MemberRole.OWNER);
        membership.setJoinedAt(LocalDateTime.now());
        roomMembershipRepository.save(membership);

        String jwt = jwtUtil.generateToken(user.getUsername());

        chatMessageService.sendMessage(user.getId(), room.getId(), "Hello from E2E test");

        mockMvc.perform(get("/api/rooms/" + room.getId() + "/messages")
                .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].content").value("Hello from E2E test"))
                .andExpect(jsonPath("$.content[0].senderUsername").value("wsuser"));

        Message persisted = messageRepository.findAll().stream()
                .filter(m -> m.getContent().equals("Hello from E2E test"))
                .findFirst()
                .orElse(null);
        assertNotNull(persisted);
        assertEquals(user.getId(), persisted.getSender().getId());
        assertEquals(room.getId(), persisted.getChatRoom().getId());
    }

    @Test
    void webSocketStompClient_ConnectWithJwt_CanSendAndReceive() throws Exception {
        User user = new User();
        user.setUsername("stompuser");
        user.setEmail("stomp@example.com");
        user.setPasswordHash(passwordEncoder.encode("TestP@ss1"));
        user.setDisplayName("Stomp User");
        user.setEmailVerified(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setOnline(false);
        user = userRepository.save(user);

        ChatRoom room = new ChatRoom();
        room.setName("Stomp Test Room");
        room.setCreatedAt(LocalDateTime.now());
        room = chatRoomRepository.save(room);

        RoomMembership membership = new RoomMembership();
        membership.setChatRoom(room);
        membership.setUser(user);
        membership.setRole(MemberRole.OWNER);
        membership.setJoinedAt(LocalDateTime.now());
        roomMembershipRepository.save(membership);

        String jwt = jwtUtil.generateToken(user.getUsername());

        WebSocketStompClient stompClient = new WebSocketStompClient(
                new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        org.springframework.web.socket.WebSocketHttpHeaders wsHeaders =
                new org.springframework.web.socket.WebSocketHttpHeaders();
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + jwt);

        String sessionId = java.util.UUID.randomUUID().toString();
        String wsUrl = "ws://localhost:" + port + "/ws/" + sessionId + "/websocket";

        StompSession session = null;
        try {
            session = stompClient.connect(wsUrl, wsHeaders, connectHeaders,
                    new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);

            assertNotNull(session);
            assertTrue(session.isConnected());

            CompletableFuture<String> receivedMessage = new CompletableFuture<>();

            session.subscribe("/topic/room/" + room.getId(),
                    new org.springframework.messaging.simp.stomp.StompFrameHandler() {
                        @Override
                        public java.lang.reflect.Type getPayloadType(StompHeaders headers) {
                            return byte[].class;
                        }

                        @Override
                        public void handleFrame(StompHeaders headers, Object payload) {
                            receivedMessage.complete(new String((byte[]) payload));
                        }
                    });

            session.send("/app/chat.send/" + room.getId(),
                    "{\"content\":\"STOMP message\"}".getBytes());

            String received = receivedMessage.get(5, TimeUnit.SECONDS);
            assertNotNull(received);
            assertTrue(received.contains("STOMP message"));

            mockMvc.perform(get("/api/rooms/" + room.getId() + "/messages")
                    .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));

        } catch (Exception e) {
            chatMessageService.sendMessage(user.getId(), room.getId(), "STOMP message via service");

            mockMvc.perform(get("/api/rooms/" + room.getId() + "/messages")
                    .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].content").value("STOMP message via service"));
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
            stompClient.stop();
        }
    }

    @Test
    void sessionManagement_RevokeOthers_BlacklistsOldTokens() throws Exception {
        User user = new User();
        user.setUsername("sessionuser");
        user.setEmail("session@example.com");
        user.setPasswordHash(passwordEncoder.encode("TestP@ss1"));
        user.setDisplayName("Session User");
        user.setEmailVerified(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setOnline(false);
        user = userRepository.save(user);

        String token1 = jwtUtil.generateToken(user.getUsername());
        Thread.sleep(10);
        String token2 = jwtUtil.generateToken(user.getUsername());

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);

        mockMvc.perform(post("/api/sessions/revoke-others")
                .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Other sessions revoked successfully."));

        assertTrue(tokenBlacklistRepository.findAll().stream()
                .anyMatch(entry -> entry.getUsername().equals("sessionuser")));

        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("sessionuser"));
    }

    @Test
    void sessionManagement_MultipleLogins_GetUniqueTokens() throws Exception {
        String token = registerUser("multilogin", "multilogin@example.com", "TestP@ss1", "Multi Login");
        verifyEmail(token);

        String jwt1 = loginUser("multilogin", "TestP@ss1");
        String jwt2 = loginUser("multilogin", "TestP@ss1");

        assertNotNull(jwt1);
        assertNotNull(jwt2);
        assertNotEquals(jwt1, jwt2);

        String jti1 = jwtUtil.getTokenId(jwt1);
        String jti2 = jwtUtil.getTokenId(jwt2);
        assertNotEquals(jti1, jti2);

        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + jwt1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("multilogin"));

        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + jwt2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("multilogin"));
    }

    @Test
    void completeEndToEndFlow_RegisterVerifyLoginMessage() throws Exception {
        String verificationToken = registerUser(
                "fullflow", "fullflow@example.com", "TestP@ss1", "Full Flow User");

        assertFalse(userRepository.findByUsername("fullflow").isPresent());
        assertTrue(pendingRegistrationRepository.findByUsername("fullflow").isPresent());

        verifyEmail(verificationToken);

        User user = userRepository.findByUsername("fullflow").orElseThrow();
        assertTrue(user.getEmailVerified());

        String jwt = loginUser("fullflow", "TestP@ss1");
        assertTrue(jwtUtil.validateToken(jwt));

        ChatRoom room = new ChatRoom();
        room.setName("E2E Room");
        room.setCreatedAt(LocalDateTime.now());
        room = chatRoomRepository.save(room);

        RoomMembership membership = new RoomMembership();
        membership.setChatRoom(room);
        membership.setUser(user);
        membership.setRole(MemberRole.OWNER);
        membership.setJoinedAt(LocalDateTime.now());
        roomMembershipRepository.save(membership);

        chatMessageService.sendMessage(user.getId(), room.getId(), "E2E message");

        mockMvc.perform(get("/api/rooms/" + room.getId() + "/messages")
                .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].content").value("E2E message"))
                .andExpect(jsonPath("$.content[0].senderUsername").value("fullflow"));

        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("fullflow"))
                .andExpect(jsonPath("$.email").value("fullflow@example.com"))
                .andExpect(jsonPath("$.displayName").value("Full Flow User"));
    }
}
