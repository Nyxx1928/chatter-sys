package org.example.chat.service;

import org.example.chat.entity.PasswordResetToken;
import org.example.chat.entity.User;
import org.example.chat.repository.PasswordResetTokenRepository;
import org.example.chat.repository.UserRepository;
import org.example.chat.util.SecurityAuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class ForgotPasswordService {

    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordService.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final RateLimiterService rateLimiterService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecurityAuditLogger auditLogger;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public ForgotPasswordService(UserRepository userRepository,
                                  PasswordResetTokenRepository tokenRepository,
                                  RateLimiterService rateLimiterService,
                                  PasswordEncoder passwordEncoder,
                                  EmailService emailService,
                                  SecurityAuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.rateLimiterService = rateLimiterService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.auditLogger = auditLogger;
    }

    @Transactional
    public void initiateReset(String email) {
        logger.info("Password reset requested for email: {}", email);

        rateLimiterService.checkForgotPassword(email);

        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            logger.warn("Password reset requested for unknown email: {}", email);
            return;
        }

        User user = userOpt.get();

        tokenRepository.deleteByUser(user);

        String token = generateToken();
        PasswordResetToken resetToken = new PasswordResetToken(user, token);
        tokenRepository.save(resetToken);

        String resetUrl = buildResetUrl(token);
        boolean emailSent = emailService.sendPasswordResetEmail(user.getEmail(), resetUrl, user.getUsername());

        if (emailSent) {
            logger.info("Password reset email sent to: {}", email);
        } else {
            logger.warn("Password reset email failed to send to: {}", email);
        }
    }

    @Transactional
    public void resetPassword(String tokenStr, String newPassword) {
        logger.info("Password reset attempt with token");

        if (tokenStr == null || tokenStr.isBlank()) {
            throw new IllegalArgumentException("Reset token is required");
        }

        PasswordResetToken token = tokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        if (token.getUsed()) {
            auditLogger.logAuthorizationFailure(token.getUser().getId(), null,
                    "Password reset token already used");
            throw new IllegalArgumentException("This reset link has already been used");
        }

        if (token.isExpired()) {
            auditLogger.logAuthorizationFailure(token.getUser().getId(), null,
                    "Password reset token expired");
            throw new IllegalArgumentException("This reset link has expired. Please request a new one.");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

        logger.info("Password reset successfully for user: {}", user.getUsername());
    }

    private String generateToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String buildResetUrl(String token) {
        return frontendUrl + "/reset-password?token=" + token;
    }
}
