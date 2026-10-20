package com.mentify.common.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "mentify.notifications.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
@Profile("!test")
public class NotificationEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Executor notificationEventExecutor;
    private final String topic;
    private final String sourceService;

    public NotificationEventPublisher(
            KafkaTemplate<String, String> notificationKafkaTemplate,
            @Qualifier("notificationEventExecutor") Executor notificationEventExecutor,
            ObjectMapper objectMapper,
            @Value("${mentify.notifications.kafka.topic:mentify.notification-events}") String topic,
            @Value("${spring.application.name:unknown-service}") String sourceService
    ) {
        this.kafkaTemplate = notificationKafkaTemplate;
        this.notificationEventExecutor = notificationEventExecutor;
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.sourceService = sourceService;
    }

    public void publishForCurrentUser(String type, String title, String message) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = authentication instanceof JwtAuthenticationToken jwtAuthentication
                ? jwtAuthentication.getToken()
                : authentication != null && authentication.getPrincipal() instanceof Jwt principal
                        ? principal
                        : null;

        if (jwt == null || !StringUtils.hasText(jwt.getSubject())) {
            log.warn("Skipping notification event because the current request has no JWT subject");
            return;
        }

        String email = jwt.getClaimAsString("email");
        publishForRecipient(
                jwt.getSubject(),
                email,
                type,
                title,
                message
        );
    }

    public void publishForRecipient(
            String recipientUserId,
            String recipientEmail,
            String type,
            String title,
            String message
    ) {
        if (!StringUtils.hasText(recipientUserId)) {
            log.warn("Skipping notification event because the recipient has no user ID");
            return;
        }

        publish(new NotificationEvent(
                UUID.randomUUID(),
                recipientUserId,
                StringUtils.hasText(recipientEmail) ? recipientEmail : null,
                type,
                title,
                message,
                sourceService,
                Instant.now()
        ));
    }

    public void publish(NotificationEvent event) {
        final String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            log.error("Could not serialize notification event {}", event.eventId(), exception);
            return;
        }

        try {
            notificationEventExecutor.execute(() -> send(event, payload));
        } catch (RejectedExecutionException exception) {
            log.error("Could not queue notification event {} because the publisher is saturated",
                    event.eventId(), exception);
        }
    }

    private void send(NotificationEvent event, String payload) {
        try {
            kafkaTemplate.send(topic, event.recipientUserId(), payload)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            log.error("Could not publish notification event {} to Kafka", event.eventId(), exception);
                        }
                    });
        } catch (RuntimeException exception) {
            log.error("Could not publish notification event {} to Kafka", event.eventId(), exception);
        }
    }
}
