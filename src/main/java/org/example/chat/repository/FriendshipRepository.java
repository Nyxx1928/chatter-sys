package org.example.chat.repository;

import org.example.chat.entity.Friendship;
import org.example.chat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT f FROM Friendship f WHERE (f.userA = :user1 AND f.userB = :user2) OR (f.userA = :user2 AND f.userB = :user1)")
    Optional<Friendship> findBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);

    List<Friendship> findByUserAOrUserB(User userA, User userB);
}
