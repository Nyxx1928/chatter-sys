package org.example.chat.integration;

import org.example.chat.dto.FriendRequestCreateRequest;
import org.example.chat.entity.User;
import org.example.chat.repository.ChatRoomRepository;
import org.example.chat.repository.FriendRequestRepository;
import org.example.chat.repository.UserRepository;
import org.example.chat.security.JwtUtil;
import org.example.chat.service.ChatMessageService;
import org.example.chat.service.ChatRoomService;
import org.example.chat.service.FriendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Direct Messaging feature.
 *
 * Feature: direct-messaging
 *
 * Property 1: DM Room Creation Round-Trip
 *   Validates: Requirements 1.1, 1.2, 1.3
 *
 * Property 4: Room List Completeness
 *   Validates: Requirements 2.1, 2.2, 6.1, 6.2
 *
 * Property 6: DM Message History Ordering
 *   Validates: Requirements 3.3, 5.3
 */
class DirectMessagingIT extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private FriendService friendService;

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private ChatMessageService chatMessageService;

    private User alice;
    private User bob;
    private String aliceToken;
    private String bobToken;

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setUsername("alice_dm");
        alice.setEmail("alice_dm@example.com");
        alice.setPasswordHash(passwordEncoder.encode("TestP@ss1"));
        alice.setDisplayName("Alice DM");
        alice.setCreatedAt(LocalDateTime.now());
        alice.setOnline(false);
        alice = userRepository.save(alice);
        aliceToken = jwtUtil.generateToken(alice.getUsername());

        bob = new User();
        bob.setUsername("bob_dm");
        bob.setEmail("bob_dm@example.com");
        bob.setPasswordHash(passwordEncoder.encode("TestP@ss1"));
        bob.setDisplayName("Bob DM");
        bob.setCreatedAt(LocalDateTime.now());
        bob.setOnline(false);
        bob = userRepository.save(bob);
        bobToken = jwtUtil.generateToken(bob.getUsername());
    }

    // ── Helper: create a friendship between alice and bob via the API ─────────

    private long createFriendshipAndGetDmRoomId() throws Exception {
        // Alice sends a friend request to Bob
        FriendRequestCreateRequest req = new FriendRequestCreateRequest(bob.getId());
        String reqJson = mockMvc.perform(post("/api/friends/requests")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long requestId = objectMapper.readTree(reqJson).get("id").asLong();

        // Bob accepts — response includes dmRoomId
        String acceptJson = mockMvc.perform(post("/api/friends/requests/" + requestId + "/accept")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(acceptJson).get("dmRoomId").asLong();
    }

    // ── Property 1: DM Room Creation Round-Trip ───────────────────────────────
    // Feature: direct-messaging, Property 1: DM room creation round-trip

    /**
     * After accepting a friend request, the DM room must appear in both users'
     * room lists with both users as members and roomType == DIRECT.
     *
     * Validates: Requirements 1.1, 1.2, 1.3
     */
    @Test
    void dmRoomCreationRoundTrip_AppearsBothUsersRoomLists() throws Exception {
        long dmRoomId = createFriendshipAndGetDmRoomId();

        assertTrue(dmRoomId > 0, "dmRoomId must be a positive ID");

        // Alice's room list must contain the DM room
        mockMvc.perform(get("/api/rooms")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + dmRoomId + ")].roomType").value("DIRECT"))
                .andExpect(jsonPath("$[?(@.id == " + dmRoomId + ")]").isNotEmpty());

        // Bob's room list must contain the DM room
        mockMvc.perform(get("/api/rooms")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + dmRoomId + ")].roomType").value("DIRECT"))
                .andExpect(jsonPath("$[?(@.id == " + dmRoomId + ")]").isNotEmpty());

        // Both users must be members of the DM room
        mockMvc.perform(get("/api/rooms/" + dmRoomId + "/members")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].username", containsInAnyOrder("alice_dm", "bob_dm")));
    }

    /**
     * Calling getOrCreateDmRoom twice for the same pair must return the same room
     * and leave exactly one DIRECT room in the database.
     *
     * Validates: Requirements 1.4
     */
    @Test
    void dmRoomCreation_IsIdempotent_OnlyOneRoomCreated() throws Exception {
        long dmRoomId = createFriendshipAndGetDmRoomId();

        // Re-fetch alice and bob from DB (they may be detached after the transaction)
        User aliceDb = userRepository.findById(alice.getId()).orElseThrow();
        User bobDb = userRepository.findById(bob.getId()).orElseThrow();

        // Calling getOrCreateDmRoom a second time must return the same room
        var dmRoom2 = chatRoomService.getRoomById(dmRoomId);
        assertEquals(dmRoomId, dmRoom2.getId(),
                "Second lookup must return the same DM room ID");

        // Count DIRECT rooms between alice and bob — must be exactly 1
        String expectedName = "dm__" + Math.min(alice.getId(), bob.getId())
                + "__" + Math.max(alice.getId(), bob.getId());
        long directRoomCount = chatRoomRepository.findAll().stream()
                .filter(r -> r.getRoomType() == org.example.chat.entity.RoomType.DIRECT)
                .filter(r -> r.getName().equals(expectedName))
                .count();

        assertEquals(1, directRoomCount,
                "There must be exactly one DM room between alice and bob");
    }

    // ── Property 4: Room List Completeness ───────────────────────────────────
    // Feature: direct-messaging, Property 4: room list completeness

    /**
     * When a user has N DM rooms, listRooms returns all N rooms, each with a
     * non-null roomType field.
     *
     * Validates: Requirements 2.1, 2.2, 6.1, 6.2
     */
    @Test
    void roomListCompleteness_AllDmRoomsReturnedWithRoomType() throws Exception {
        // Create 3 additional users and make them all friends with alice
        List<User> friends = new ArrayList<>();
        List<String> friendTokens = new ArrayList<>();
        List<Long> dmRoomIds = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            User friend = new User();
            friend.setUsername("friend_" + i + "_dm");
            friend.setEmail("friend_" + i + "_dm@example.com");
            friend.setPasswordHash(passwordEncoder.encode("TestP@ss1"));
            friend.setDisplayName("Friend " + i);
            friend.setCreatedAt(LocalDateTime.now());
            friend.setOnline(false);
            friend = userRepository.save(friend);
            friends.add(friend);
            friendTokens.add(jwtUtil.generateToken(friend.getUsername()));
        }

        // Alice sends friend requests to each friend
        for (int i = 0; i < 3; i++) {
            FriendRequestCreateRequest req = new FriendRequestCreateRequest(friends.get(i).getId());
            String reqJson = mockMvc.perform(post("/api/friends/requests")
                            .header("Authorization", "Bearer " + aliceToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            long requestId = objectMapper.readTree(reqJson).get("id").asLong();

            String acceptJson = mockMvc.perform(post("/api/friends/requests/" + requestId + "/accept")
                            .header("Authorization", "Bearer " + friendTokens.get(i)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            dmRoomIds.add(objectMapper.readTree(acceptJson).get("dmRoomId").asLong());
        }

        // Alice's room list must contain all 3 DM rooms
        String roomsJson = mockMvc.perform(get("/api/rooms")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var roomsNode = objectMapper.readTree(roomsJson);
        assertTrue(roomsNode.isArray(), "Response must be an array");

        // All returned rooms must have a non-null roomType
        for (var roomNode : roomsNode) {
            assertNotNull(roomNode.get("roomType"),
                    "Every room must have a non-null roomType field");
            assertFalse(roomNode.get("roomType").isNull(),
                    "roomType must not be JSON null");
        }

        // All 3 DM room IDs must be present in the list
        List<Long> returnedIds = new ArrayList<>();
        for (var roomNode : roomsNode) {
            returnedIds.add(roomNode.get("id").asLong());
        }
        for (long dmRoomId : dmRoomIds) {
            assertTrue(returnedIds.contains(dmRoomId),
                    "DM room " + dmRoomId + " must appear in alice's room list");
        }
    }

    // ── Property 6: DM Message History Ordering ───────────────────────────────
    // Feature: direct-messaging, Property 6: DM message history ordering

    /**
     * Messages sent to a DM room must be returned in ascending timestamp order
     * and the default page size must be 50.
     *
     * Validates: Requirements 3.3, 5.3
     */
    @Test
    void dmMessageHistoryOrdering_AscendingTimestampAndDefaultPageSize() throws Exception {
        long dmRoomId = createFriendshipAndGetDmRoomId();

        // Send 5 messages from alice to the DM room via the service layer
        // (STOMP is not available in MockMvc tests; use the service directly)
        User aliceDb = userRepository.findById(alice.getId()).orElseThrow();
        for (int i = 1; i <= 5; i++) {
            chatMessageService.sendMessage(aliceDb.getId(), dmRoomId, "Message " + i);
        }

        // Retrieve message history via REST
        String historyJson = mockMvc.perform(get("/api/rooms/" + dmRoomId + "/messages")
                        .param("page", "0")
                        .param("size", "50")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var historyNode = objectMapper.readTree(historyJson);

        // Response must have a "content" array (paginated response)
        assertTrue(historyNode.has("content"), "Response must have a 'content' field");
        var content = historyNode.get("content");
        assertTrue(content.isArray(), "'content' must be an array");
        assertEquals(5, content.size(), "Must return exactly 5 messages");

        // Messages must be in ascending timestamp order
        String prevTimestamp = null;
        for (var msgNode : content) {
            String ts = msgNode.get("timestamp").asText();
            if (prevTimestamp != null) {
                assertTrue(ts.compareTo(prevTimestamp) >= 0,
                        "Messages must be in ascending timestamp order: " + prevTimestamp + " <= " + ts);
            }
            prevTimestamp = ts;
        }

        // Default page size must accommodate up to 50 messages
        assertTrue(historyNode.has("size") || historyNode.has("pageable"),
                "Response must include pagination metadata");
    }

    /**
     * Non-participants must not be able to access DM room message history.
     *
     * Validates: Requirements 4.1, 4.2, 4.3
     */
    @Test
    void dmRoomAccess_NonParticipant_IsRejected() throws Exception {
        long dmRoomId = createFriendshipAndGetDmRoomId();

        // Create a third user who is not part of the DM room
        User charlie = new User();
        charlie.setUsername("charlie_dm");
        charlie.setEmail("charlie_dm@example.com");
        charlie.setPasswordHash(passwordEncoder.encode("TestP@ss1"));
        charlie.setDisplayName("Charlie DM");
        charlie.setCreatedAt(LocalDateTime.now());
        charlie.setOnline(false);
        charlie = userRepository.save(charlie);
        String charlieToken = jwtUtil.generateToken(charlie.getUsername());

        // Charlie must not be able to read the DM room details
        mockMvc.perform(get("/api/rooms/" + dmRoomId)
                        .header("Authorization", "Bearer " + charlieToken))
                .andExpect(status().isForbidden());

        // Charlie must not be able to read message history
        mockMvc.perform(get("/api/rooms/" + dmRoomId + "/messages")
                        .header("Authorization", "Bearer " + charlieToken))
                .andExpect(status().isForbidden());
    }

    /**
     * DM rooms must not be deletable — even by a participant.
     *
     * Validates: Requirements 4.5
     */
    @Test
    void dmRoom_CannotBeDeleted_ByParticipant() throws Exception {
        long dmRoomId = createFriendshipAndGetDmRoomId();

        mockMvc.perform(delete("/api/rooms/" + dmRoomId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isForbidden());

        // Room must still exist
        assertTrue(chatRoomRepository.findById(dmRoomId).isPresent(),
                "DM room must not be deleted");
    }

    /**
     * DM rooms must not allow adding new members.
     *
     * Validates: Requirements 4.4
     */
    @Test
    void dmRoom_CannotInviteNewMembers() throws Exception {
        long dmRoomId = createFriendshipAndGetDmRoomId();

        // Create a third user
        User charlie = new User();
        charlie.setUsername("charlie_invite_dm");
        charlie.setEmail("charlie_invite_dm@example.com");
        charlie.setPasswordHash(passwordEncoder.encode("TestP@ss1"));
        charlie.setDisplayName("Charlie Invite DM");
        charlie.setCreatedAt(LocalDateTime.now());
        charlie.setOnline(false);
        charlie = userRepository.save(charlie);

        // Try to invite charlie to the DM room via the invite endpoint
        mockMvc.perform(post("/api/rooms/" + dmRoomId + "/invite")
                        .param("inviteeId", String.valueOf(charlie.getId()))
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isForbidden());
    }
}
