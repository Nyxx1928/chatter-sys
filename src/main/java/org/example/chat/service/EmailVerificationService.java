package org.example.chat.service;

import org.example.chat.entity.User;
import org.example.chat.entity.VerificationToken;
import org.example.chat.repository.UserRepository;
import org.example.chat.repository.VerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class EmailVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);

    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final BrevoEmailService brevoEmailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public EmailVerificationService(VerificationTokenRepository tokenRepository,
                                    UserRepository userRepository,
                                    BrevoEmailService brevoEmailService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.brevoEmailService = brevoEmailService;
    }

    public record VerificationDispatchResult(String token, String verificationUrl, boolean emailSent) {}

    @Transactional
    public VerificationDispatchResult createAndSendToken(User user) {
        tokenRepository.deleteByUser(user);

        VerificationToken token = new VerificationToken(user);
        tokenRepository.save(token);

        String verificationUrl = buildVerificationUrl(token.getToken());
        BrevoEmailService.EmailResult emailResult =
                brevoEmailService.sendVerificationEmail(user.getEmail(), verificationUrl);
        boolean emailSent = emailResult.success();
        if (!emailSent) {
            logger.warn("Verification email was not sent for user {}. Falling back to verificationUrl only. Error: {}",
                    user.getUsername(), emailResult.errorMessage());
        }

        return new VerificationDispatchResult(token.getToken(), verificationUrl, emailSent);
    }

    public String buildVerificationUrl(String token) {
        return baseUrl + "/api/auth/verify-email?token=" + token;
    }

    @Transactional
    public void verifyEmail(String tokenStr) {
        if (tokenStr == null || tokenStr.isBlank()) {
            throw new IllegalArgumentException("Verification token is required");
        }

        VerificationToken token = tokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        if (token.getUsed()) {
            throw new IllegalArgumentException("Verification token has already been used");
        }

        if (token.isExpired()) {
            throw new IllegalArgumentException("Verification token has expired. Request a new one.");
        }

        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

        logger.info("Email verified for user: {}", user.getUsername());
    }

    @Transactional
    public void resendVerification(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            logger.warn("Resend verification requested for unknown email: {}", email);
            return;
        }

        User user = userOpt.get();

        if (user.getEmailVerified()) {
            logger.warn("Resend verification requested for already verified user: {}", user.getUsername());
            return;
        }

        createAndSendToken(user);
    }

    public boolean isEmailVerified(User user) {
        return Boolean.TRUE.equals(user.getEmailVerified());
    }
}
