package org.example.chat.service;

import org.example.chat.repository.TokenBlacklistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Periodically cleans up expired token blacklist entries.
 * Prevents unbounded growth of the token_blacklist table.
 */
@Service
public class TokenBlacklistCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistCleanupService.class);

    private final TokenBlacklistRepository tokenBlacklistRepository;

    public TokenBlacklistCleanupService(TokenBlacklistRepository tokenBlacklistRepository) {
        this.tokenBlacklistRepository = tokenBlacklistRepository;
    }

    /**
     * Removes expired blacklist entries daily.
     */
    @Scheduled(fixedRate = 86400000) // 24 hours
    @Transactional
    public void cleanupExpiredEntries() {
        LocalDateTime cutoff = LocalDateTime.now();
        int deleted = tokenBlacklistRepository.deleteExpiredBefore(cutoff);
        if (deleted > 0) {
            logger.info("Cleaned up {} expired token blacklist entries", deleted);
        }
    }
}
