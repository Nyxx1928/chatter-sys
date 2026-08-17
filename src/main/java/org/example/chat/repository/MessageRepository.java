package org.example.chat.repository;

import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    Page<Message> findByChatRoomOrderByTimestampDesc(ChatRoom room, Pageable pageable);
    
    List<Message> findByChatRoomAndTimestampAfter(ChatRoom room, LocalDateTime timestamp);

    @Modifying
    @Query("DELETE FROM Message m WHERE m.chatRoom = :room")
    void deleteByChatRoom(@Param("room") ChatRoom room);
}
