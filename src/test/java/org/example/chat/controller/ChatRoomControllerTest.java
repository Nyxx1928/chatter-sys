package org.example.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.chat.config.WebMvcTestConfig;
import org.example.chat.dto.CreateRoomRequest;
import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.RoomMembership;
import org.example.chat.entity.User;
import org.example.chat.exception.RoomNotFoundException;
import org.example.chat.repository.RoomMembershipRepository;
import org.example.chat.repository.UserRepository;
import org.example.chat.service.ChatRoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ChatRoomController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
@Import({WebMvcTestConfig.class, org.example.chat.exception.GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for unit tests
@ActiveProfiles("test")
class ChatRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatRoomService chatRoomService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RoomMembershipRepository roomMembershipRepository;

    private User testUser;
    private ChatRoom testRoom;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setDisplayName("Test User");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setOnline(true);

        testRoom = new ChatRoom();
        testRoom.setId(1L);
        testRoom.setName("Test Room");
        testRoom.setDescription("A test chat room");
        testRoom.setCreatedAt(LocalDateTime.now());
        testRoom.setCreatedBy(testUser);
    }

    @Test
    @WithMockUser(username = "testuser")
    void createRoom_ValidRequest_ReturnsCreated() throws Exception {
        CreateRoomRequest request = new CreateRoomRequest("Test Room", "A test chat room");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.createRoom(eq("Test Room"), eq("A test chat room"), eq(1L)))
            .thenReturn(testRoom);

        mockMvc.perform(post("/api/rooms")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Test Room"))
            .andExpect(jsonPath("$.description").value("A test chat room"))
            .andExpect(jsonPath("$.createdBy.username").value("testuser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void createRoom_InvalidRequest_ReturnsBadRequest() throws Exception {
        CreateRoomRequest request = new CreateRoomRequest("", "Description");

        mockMvc.perform(post("/api/rooms")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void createRoom_DuplicateName_ThrowsException() throws Exception {
        CreateRoomRequest request = new CreateRoomRequest("Test Room", "A test chat room");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.createRoom(anyString(), anyString(), anyLong()))
            .thenThrow(new IllegalArgumentException("Room name already exists"));

        mockMvc.perform(post("/api/rooms")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void listRooms_ReturnsAllRooms() throws Exception {
        ChatRoom room2 = new ChatRoom();
        room2.setId(2L);
        room2.setName("Room 2");
        room2.setDescription("Second room");
        room2.setCreatedAt(LocalDateTime.now());
        room2.setCreatedBy(testUser);

        List<ChatRoom> rooms = Arrays.asList(testRoom, room2);
        when(chatRoomService.listRooms()).thenReturn(rooms);

        mockMvc.perform(get("/api/rooms"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("Test Room"))
            .andExpect(jsonPath("$[1].name").value("Room 2"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getRoomById_ExistingRoom_ReturnsRoom() throws Exception {
        // Mock the user repository
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        // Mock the room service
        when(chatRoomService.getRoomById(1L)).thenReturn(testRoom);
        
        // Mock the membership check
        RoomMembership membership = new RoomMembership();
        membership.setUser(testUser);
        membership.setChatRoom(testRoom);
        when(roomMembershipRepository.findByUserAndChatRoom(testUser, testRoom))
            .thenReturn(Optional.of(membership));

        mockMvc.perform(get("/api/rooms/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Test Room"))
            .andExpect(jsonPath("$.description").value("A test chat room"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getRoomById_NonExistingRoom_ThrowsException() throws Exception {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatRoomService.getRoomById(999L))
            .thenThrow(new RoomNotFoundException(999L));

        mockMvc.perform(get("/api/rooms/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    void getRoomMembers_ExistingRoom_ReturnsMembers() throws Exception {
        User member2 = new User();
        member2.setId(2L);
        member2.setUsername("member2");
        member2.setEmail("member2@example.com");
        member2.setDisplayName("Member Two");
        member2.setCreatedAt(LocalDateTime.now());
        member2.setOnline(false);

        List<User> members = Arrays.asList(testUser, member2);
        when(chatRoomService.getRoomMembers(1L)).thenReturn(members);

        mockMvc.perform(get("/api/rooms/1/members"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].username").value("testuser"))
            .andExpect(jsonPath("$[1].username").value("member2"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getRoomMembers_NonExistingRoom_ThrowsException() throws Exception {
        when(chatRoomService.getRoomMembers(999L))
            .thenThrow(new RoomNotFoundException(999L));

        mockMvc.perform(get("/api/rooms/999/members"))
            .andExpect(status().isNotFound());
    }
}
