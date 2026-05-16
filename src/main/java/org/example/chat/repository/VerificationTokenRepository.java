package org.example.chat.repository;

import org.example.chat.entity.User;
import org.example.chat.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByToken(String token);

    Optional<VerificationToken> findByUserAndUsedFalse(User user);

    void deleteByUser(User user);

    @Modifying
    @Query("""
        DELETE FROM VerificationToken vt
        WHERE vt.user.id IN (SELECT u.id FROM User u WHERE u.emailVerified = false)
        """)
    int deleteByUserEmailVerifiedFalse();
}
