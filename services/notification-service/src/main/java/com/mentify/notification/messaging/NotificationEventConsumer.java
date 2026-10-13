package com.mentify.notification.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.common.notification.NotificationEvent;
import com.mentify.notification.service.NotificationIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mentify.notifications.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NotificationEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationIngestionService notificationIngestionService;

    @KafkaListener(
            topics = "${mentify.notifications.kafka.topic:mentify.notification-events}",
            groupId = "${mentify.notifications.kafka.group-id:notification-service}"
    )
    public void consume(String payload) {
        try {
            NotificationEvent event = objectMapper.readValue(payload, NotificationEvent.class);
            notificationIngestionService.ingest(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Received an invalid notification event", exception);
        }
    }
}
