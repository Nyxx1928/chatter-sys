package org.example.chat.service;

import org.example.chat.entity.Message;
import org.example.chat.entity.PushToken;
import org.example.chat.entity.User;
import org.example.chat.repository.PushTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class PushNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);
    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";
    private static final int MAX_BODY_LENGTH = 120;

    private final PushTokenRepository pushTokenRepository;
    private final UserPresenceService userPresenceService;
    private final ChatRoomService chatRoomService;
    private final WebClient webClient;

    public PushNotificationService(PushTokenRepository pushTokenRepository,
                                   UserPresenceService userPresenceService,
                                   ChatRoomService chatRoomService,
                                   WebClient.Builder webClientBuilder) {
        this.pushTokenRepository = pushTokenRepository;
        this.userPresenceService = userPresenceService;
        this.chatRoomService = chatRoomService;
        this.webClient = webClientBuilder.build();
    }

    public void sendPushIfOffline(Message message, Long roomId) {
        try {
            List<User> members = chatRoomService.getRoomMembers(roomId);

            for (User member : members) {
                if (member.getId().equals(message.getSender().getId())) continue;

                if (userPresenceService.isOnline(member.getId())) continue;

                List<PushToken> tokens = pushTokenRepository.findByUserId(member.getId());
                if (tokens.isEmpty()) continue;

                String title = message.getSender().getDisplayName();
                String body = message.getContent();
                if (body.length() > MAX_BODY_LENGTH) {
                    body = body.substring(0, MAX_BODY_LENGTH);
                }

                for (PushToken token : tokens) {
                    sendExpoPush(token.getPushToken(), title, body, roomId);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to send push notification for message {} in room {}", message.getId(), roomId, e);
        }
    }

    private void sendExpoPush(String pushToken, String title, String body, Long roomId) {
        Map<String, Object> payload = Map.of(
                "to", pushToken,
                "title", title,
                "body", body,
                "data", Map.of("roomId", roomId, "type", "new_message"),
                "sound", "default",
                "priority", "high"
        );

        try {
            String response = webClient.post()
                    .uri(EXPO_PUSH_URL)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));

            logger.debug("Expo push response for token {}: {}", pushToken, response);
        } catch (Exception e) {
            logger.error("Failed to send Expo push to token {}", pushToken, e);
        }
    }
}
