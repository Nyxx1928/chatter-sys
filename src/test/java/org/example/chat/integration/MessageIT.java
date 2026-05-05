package org.example.chat.integration;

import org.example.chat.entity.*;
import org.example.chat.repository.ChatRoomRepository;
import org.example.chat.repository.MessageRepository;
import org.example.chat.repository.RoomMembershipRepository;
import org.example.chat.repository.UserRepository;
import org.example.chat.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for message operations.
 * Tests message history retrieval with real database.
 */
class MessageIT extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private RoomMembershipRepository roomMembershipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private User testUser;
    private ChatRoom testRoom;
    private String authToken;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUsername("messageuser");
        testUser.setEmail("message@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("password123"));
        testUser.setDisplayName("Message Test User");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setOnline(false);
        testUser = userRepository.save(testUser);

        // Create test room
        testRoom = new ChatRoom();
        testRoom.setName("Test Room");
        testRoom.setCreatedAt(LocalDateTime.now());
        testRoom = chatRoomRepository.save(testRoom);

        // Add user as member
        RoomMembership membership = new RoomMembership();
        membership.setChatRoom(testRoom);
        membership.setUser(testUser);
        membership.setRole(MemberRole.OWNER);
        membership.setJoinedAt(LocalDateTime.now());
        roomMembershipRepository.save(membership);

        // Generate auth token
        authToken = jwtUtil.generateToken(testUser.getUsername());
    }

    @Test
    void getMessageHistory_RoomWithMessages_ReturnsMessages() throws Exception {
        // Create messages
        Message message1 = new Message();
        message1.setChatRoom(testRoom);
        message1.setSender(testUser);
        message1.setContent("First message");
        message1.setMessageType(MessageType.TEXT);
        message1.setTimestamp(LocalDateTime.now().minusMinutes(10));
        messageRepository.save(message1);

        Message message2 = new Message();
        message2.setChatRoom(testRoom);
        message2.setSender(testUser);
        message2.setContent("Second message");
        message2.setMessageType(MessageType.TEXT);
        message2.setTimestamp(LocalDateTime.now().minusMinutes(5));
        messageRepository.save(message2);

        Message message3 = new Message();
        message3.setChatRoom(testRoom);
        message3.setSender(testUser);
        message3.setContent("Third message");
        message3.setMessageType(MessageType.TEXT);
        message3.setTimestamp(LocalDateTime.now());
        messageRepository.save(message3);

        // Get message history
        mockMvc.perform(get("/api/rooms/" + testRoom.getId() + "/messages")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].content").value("First message"))
                .andExpect(jsonPath("$.content[1].content").value("Second message"))
                .andExpect(jsonPath("$.content[2].content").value("Third message"))
                .andExpect(jsonPath("$.content[*].senderUsername", everyItem(is("messageuser"))));
    }

    @Test
    void getMessageHistory_EmptyRoom_ReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/rooms/" + testRoom.getId() + "/messages")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void getMessageHistory_WithLimit_ReturnsLimitedMessages() throws Exception {
        // Create 10 messages
        for (int i = 1; i <= 10; i++) {
            Message message = new Message();
            message.setChatRoom(testRoom);
            message.setSender(testUser);
            message.setContent("Message " + i);
            message.setMessageType(MessageType.TEXT);
            message.setTimestamp(LocalDateTime.now().minusMinutes(10 - i));
            messageRepository.save(message);
        }

        // Get only 5 messages using Spring's 'size' parameter
        mockMvc.perform(get("/api/rooms/" + testRoom.getId() + "/messages")
                .header("Authorization", "Bearer " + authToken)
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)));
    }

    @Test
    void getMessageHistory_UserNotMember_AutoJoinsAndReturnsMessages() throws Exception {
        // Create another user
        User otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");
        otherUser.setPasswordHash(passwordEncoder.encode("password123"));
        otherUser.setDisplayName("Other User");
        otherUser.setCreatedAt(LocalDateTime.now());
        otherUser.setOnline(false);
        otherUser = userRepository.save(otherUser);

        String otherToken = jwtUtil.generateToken(otherUser.getUsername());

        // Get messages as non-member (auto-joins)
        mockMvc.perform(get("/api/rooms/" + testRoom.getId() + "/messages")
                .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        assertTrue(roomMembershipRepository.findByUserAndChatRoom(otherUser, testRoom).isPresent());
    }

    @Test
    void getMessageHistory_NonexistentRoom_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/rooms/99999/messages")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMessageHistory_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/rooms/" + testRoom.getId() + "/messages"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void messageHistory_MultipleUsers_ShowsCorrectSenders() throws Exception {
        // Create another user
        User user2 = new User();
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");
        user2.setPasswordHash(passwordEncoder.encode("password123"));
        user2.setDisplayName("User Two");
        user2.setCreatedAt(LocalDateTime.now());
        user2.setOnline(false);
        user2 = userRepository.save(user2);

        // Add user2 as member
        RoomMembership membership2 = new RoomMembership();
        membership2.setChatRoom(testRoom);
        membership2.setUser(user2);
        membership2.setRole(MemberRole.MEMBER);
        membership2.setJoinedAt(LocalDateTime.now());
        roomMembershipRepository.save(membership2);

        // Create messages from both users
        Message message1 = new Message();
        message1.setChatRoom(testRoom);
        message1.setSender(testUser);
        message1.setContent("Message from user 1");
        message1.setMessageType(MessageType.TEXT);
        message1.setTimestamp(LocalDateTime.now().minusMinutes(5));
        messageRepository.save(message1);

        Message message2 = new Message();
        message2.setChatRoom(testRoom);
        message2.setSender(user2);
        message2.setContent("Message from user 2");
        message2.setMessageType(MessageType.TEXT);
        message2.setTimestamp(LocalDateTime.now());
        messageRepository.save(message2);

        // Get message history
        mockMvc.perform(get("/api/rooms/" + testRoom.getId() + "/messages")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].senderUsername").value("messageuser"))
                .andExpect(jsonPath("$.content[0].content").value("Message from user 1"))
                .andExpect(jsonPath("$.content[1].senderUsername").value("user2"))
                .andExpect(jsonPath("$.content[1].content").value("Message from user 2"));
    }

    @Test
    void messageHistory_DifferentMessageTypes_ReturnsAllTypes() throws Exception {
        // Create messages of different types
        Message chatMessage = new Message();
        chatMessage.setChatRoom(testRoom);
        chatMessage.setSender(testUser);
        chatMessage.setContent("Chat message");
        chatMessage.setMessageType(MessageType.TEXT);
        chatMessage.setTimestamp(LocalDateTime.now().minusMinutes(3));
        messageRepository.save(chatMessage);

        Message joinMessage = new Message();
        joinMessage.setChatRoom(testRoom);
        joinMessage.setSender(testUser);
        joinMessage.setContent("User joined");
        joinMessage.setMessageType(MessageType.JOIN);
        joinMessage.setTimestamp(LocalDateTime.now().minusMinutes(2));
        messageRepository.save(joinMessage);

        Message leaveMessage = new Message();
        leaveMessage.setChatRoom(testRoom);
        leaveMessage.setSender(testUser);
        leaveMessage.setContent("User left");
        leaveMessage.setMessageType(MessageType.LEAVE);
        leaveMessage.setTimestamp(LocalDateTime.now());
        messageRepository.save(leaveMessage);

        // Get message history
        mockMvc.perform(get("/api/rooms/" + testRoom.getId() + "/messages")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].messageType").value("TEXT"))
                .andExpect(jsonPath("$.content[1].messageType").value("JOIN"))
                .andExpect(jsonPath("$.content[2].messageType").value("LEAVE"));
    }
}