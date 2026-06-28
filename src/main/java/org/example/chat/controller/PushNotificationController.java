package org.example.chat.controller;

import jakarta.validation.Valid;
import org.example.chat.dto.RegisterPushTokenRequest;
import org.example.chat.dto.UnregisterPushTokenRequest;
import org.example.chat.entity.PushToken;
import org.example.chat.entity.User;
import org.example.chat.repository.PushTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/push")
public class PushNotificationController {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationController.class);

    private final PushTokenRepository pushTokenRepository;

    public PushNotificationController(PushTokenRepository pushTokenRepository) {
        this.pushTokenRepository = pushTokenRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> registerPushToken(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody RegisterPushTokenRequest request) {

        String platform = request.getPlatform().toLowerCase();
        if (!platform.equals("ios") && !platform.equals("android")) {
            return ResponseEntity.badRequest().build();
        }

        Optional<PushToken> existing = pushTokenRepository.findByPushToken(request.getPushToken());
        if (existing.isPresent()) {
            PushToken token = existing.get();
            token.setUser(currentUser);
            token.setPlatform(platform);
            token.setUpdatedAt(LocalDateTime.now());
            pushTokenRepository.save(token);
            logger.info("Updated push token for user {}: {}", currentUser.getId(), request.getPushToken());
        } else {
            PushToken pushToken = new PushToken();
            pushToken.setUser(currentUser);
            pushToken.setPushToken(request.getPushToken());
            pushToken.setPlatform(platform);
            pushToken.setCreatedAt(LocalDateTime.now());
            pushToken.setUpdatedAt(LocalDateTime.now());
            pushTokenRepository.save(pushToken);
            logger.info("Registered push token for user {}: {}", currentUser.getId(), request.getPushToken());
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/unregister")
    public ResponseEntity<Void> unregisterPushToken(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UnregisterPushTokenRequest request) {

        pushTokenRepository.deleteByPushToken(request.getPushToken());
        logger.info("Unregistered push token for user {}: {}", currentUser.getId(), request.getPushToken());

        return ResponseEntity.ok().build();
    }
}
