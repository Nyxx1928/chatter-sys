package org.example.chat.repository;

import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.RoomType;
import org.example.chat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findByCreatedById(Long userId);
    
    @Query("SELECT DISTINCT cr FROM ChatRoom cr JOIN cr.memberships m WHERE m.user = :user")
    List<ChatRoom> findByMembersContaining(@Param("user") User user);
    
    Optional<ChatRoom> findByName(String name);

    Optional<ChatRoom> findByNameAndRoomType(String name, RoomType roomType);

    List<ChatRoom> findByRoomType(RoomType roomType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.id = :id")
    Optional<ChatRoom> findByIdForUpdate(@Param("id") Long id);
}
