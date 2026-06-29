package org.example.chat.repository;

import org.example.chat.entity.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for managing revoked JWT tokens.
 * Used for multi-device session management (JLabs3-style revoke-others).
 */
@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {

    boolean existsByTokenJti(String tokenJti);

    List<TokenBlacklist> findByUsernameAndExpiresAtAfter(String username, LocalDateTime now);

    @Modifying
    @Query("DELETE FROM TokenBlacklist t WHERE t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
