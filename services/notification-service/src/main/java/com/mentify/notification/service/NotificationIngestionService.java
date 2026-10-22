package com.mentify.notification.service;

import com.mentify.common.notification.NotificationEvent;
import com.mentify.redis.RedisCacheService;
import com.mentify.redis.RedisKey;
import com.mentify.notification.entity.EmailDeliveryStatus;
import com.mentify.notification.entity.Notification;
import com.mentify.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationIngestionService {

    private final NotificationRepository notificationRepository;
    @Autowired(required = false)
    private RedisCacheService redisCache;

    @Transactional
    public void ingest(NotificationEvent event) {
        validate(event);
        if (notificationRepository.existsByEventIdAndRecipientUserId(
                event.eventId(),
                event.recipientUserId()
        )) {
            return;
        }

        Instant now = Instant.now();
        boolean shouldSendEmail = StringUtils.hasText(event.recipientEmail());

        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setEventId(event.eventId());
        notification.setRecipientUserId(event.recipientUserId());
        notification.setRecipientEmail(shouldSendEmail ? event.recipientEmail() : null);
        notification.setType(event.type());
        notification.setTitle(event.title());
        notification.setMessage(event.message());
        notification.setSourceService(event.sourceService());
        notification.setOccurredAt(event.occurredAt());
        notification.setCreatedAt(now);
        notification.setEmailStatus(shouldSendEmail ? EmailDeliveryStatus.PENDING : EmailDeliveryStatus.NOT_REQUESTED);
        notification.setEmailAttempts(0);
        notification.setNextEmailAttemptAt(shouldSendEmail ? now : null);
        notificationRepository.save(notification);
        String notificationPrefix = RedisKey.of("mentify:v1", "notifications", event.recipientUserId());
        if (redisCache != null) {
            redisCache.evict(notificationPrefix + ":unread-count");
            redisCache.evictByPattern(notificationPrefix + ":page:*");
        }
    }

    private void validate(NotificationEvent event) {
        if (event == null
                || event.eventId() == null
                || !StringUtils.hasText(event.recipientUserId())
                || !StringUtils.hasText(event.type())
                || !StringUtils.hasText(event.title())
                || !StringUtils.hasText(event.message())
                || !StringUtils.hasText(event.sourceService())
                || event.occurredAt() == null) {
            throw new IllegalArgumentException("Notification event is missing required fields");
        }
    }
}
