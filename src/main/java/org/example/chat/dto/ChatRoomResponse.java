package org.example.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.RoomType;

import java.time.LocalDateTime;

/**
 * Response DTO for ChatRoom entity.
 * Includes basic room information and creator details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponse {
    
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private UserResponse createdBy;
    private RoomType roomType;
    
    /**
     * Creates a ChatRoomResponse from a ChatRoom entity.
     *
     * @param chatRoom the ChatRoom entity
     * @return ChatRoomResponse instance
     */
    public static ChatRoomResponse from(ChatRoom chatRoom) {
        UserResponse createdByResponse = chatRoom.getCreatedBy() != null 
            ? UserResponse.from(chatRoom.getCreatedBy()) 
            : null;
        
        return new ChatRoomResponse(
            chatRoom.getId(),
            chatRoom.getName(),
            chatRoom.getDescription(),
            chatRoom.getCreatedAt(),
            createdByResponse,
            chatRoom.getRoomType()
        );
    }
}
