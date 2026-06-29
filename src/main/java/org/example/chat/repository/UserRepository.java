package org.example.chat.repository;

import org.example.chat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    List<User> findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
            String username,
            String displayName
    );

    long countByEmailVerifiedFalse();

    /**
     * Nulls out the creator reference on all chat rooms owned by unverified users,
     * then deletes the unverified users and their dependent data.
     *
     * IMPORTANT: chat_rooms are NOT deleted — the created_by_id is merely nulled,
     * preserving group rooms that other members may be using.
     */
    @Modifying
    @Query(value = """
        UPDATE chat_rooms SET created_by_id = NULL
            WHERE created_by_id IN (SELECT id FROM users WHERE email_verified = false);
        DELETE FROM verification_tokens WHERE user_id IN (SELECT id FROM users WHERE email_verified = false);
        DELETE FROM password_reset_tokens WHERE user_id IN (SELECT id FROM users WHERE email_verified = false);
        DELETE FROM messages WHERE sender_id IN (SELECT id FROM users WHERE email_verified = false);
        DELETE FROM room_memberships WHERE user_id IN (SELECT id FROM users WHERE email_verified = false);
        DELETE FROM friendships WHERE user_a_id IN (SELECT id FROM users WHERE email_verified = false)
            OR user_b_id IN (SELECT id FROM users WHERE email_verified = false);
        DELETE FROM friend_requests WHERE requester_id IN (SELECT id FROM users WHERE email_verified = false)
            OR recipient_id IN (SELECT id FROM users WHERE email_verified = false);
        DELETE FROM users WHERE email_verified = false;
        """, nativeQuery = true)
    int deleteUnverifiedUsersAndDependents();
}
