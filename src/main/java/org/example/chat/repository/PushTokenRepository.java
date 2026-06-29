package org.example.chat.repository;

import org.example.chat.entity.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    List<PushToken> findByUserId(Long userId);

    Optional<PushToken> findByPushToken(String pushToken);

    void deleteByPushToken(String pushToken);
}
