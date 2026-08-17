package org.example.chat.repository;

import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.MemberRole;
import org.example.chat.entity.RoomMembership;
import org.example.chat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomMembershipRepository extends JpaRepository<RoomMembership, Long> {
    
    List<RoomMembership> findByChatRoom(ChatRoom room);
    
    List<RoomMembership> findByChatRoomId(Long roomId);
    
    List<RoomMembership> findByUser(User user);
    
    Optional<RoomMembership> findByUserAndChatRoom(User user, ChatRoom room);
    
    void deleteByUserAndChatRoom(User user, ChatRoom room);

    @Modifying
    @Query("DELETE FROM RoomMembership m WHERE m.chatRoom = :room")
    void deleteByChatRoom(@Param("room") ChatRoom room);

    long countByChatRoomAndRole(ChatRoom room, MemberRole role);
}
