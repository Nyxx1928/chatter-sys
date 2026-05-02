package org.example.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.chat.entity.Message;
import org.example.chat.entity.MessageType;

import java.time.LocalDateTime;

/**
 * Response DTO for Message entity.
 * Avoids exposing entity internals and provides a clean API contract.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    
    private Long id;
    private Long senderId;
    private String senderUsername;
    private String senderDisplayName;
    private Long chatRoomId;
    private String content;
    private LocalDateTime timestamp;
    private MessageType messageType;
    
    /**
     * Creates a MessageResponse from a Message entity.
     *
     * @param message the Message entity
     * @return MessageResponse instance
     */
    public static MessageResponse from(Message message) {
        return new MessageResponse(
            message.getId(),
            message.getSender().getId(),
            message.getSender().getUsername(),
            message.getSender().getDisplayName(),
            message.getChatRoom().getId(),
            message.getContent(),
            message.getTimestamp(),
            message.getMessageType()
        );
    }
}
