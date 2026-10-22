package com.mentify.notification.service;

import com.mentify.exception.ResourceNotFoundException;
import com.mentify.redis.RedisCacheService;
import com.mentify.redis.RedisKey;
import com.mentify.notification.dto.NotificationPageResponse;
import com.mentify.notification.dto.NotificationResponse;
import com.mentify.notification.entity.Notification;
import com.mentify.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationManagementService {

    private final NotificationRepository notificationRepository;
    @Autowired(required = false)
    private RedisCacheService redisCache;

    @Transactional(readOnly = true)
    public NotificationPageResponse getNotifications(String recipientUserId, int page, int size, boolean unreadOnly) {
        String pageKey = notificationPageKey(recipientUserId, page, size, unreadOnly);
        NotificationPageResponse cached = redisCache == null
                ? null
                : redisCache.get(pageKey, NotificationPageResponse.class).orElse(null);
        if (cached != null) return cached;

        PageRequest pageable = PageRequest.of(page, size);
        Page<Notification> notifications = unreadOnly
                ? notificationRepository.findByRecipientUserIdAndReadAtIsNullOrderByCreatedAtDesc(
                        recipientUserId,
                        pageable
                )
                : notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(recipientUserId, pageable);

        NotificationPageResponse response = new NotificationPageResponse(
                notifications.map(this::toResponse).getContent(),
                notifications.getNumber(),
                notifications.getSize(),
                notifications.getTotalElements(),
                notifications.getTotalPages(),
                notificationRepository.countByRecipientUserIdAndReadAtIsNull(recipientUserId)
        );
        if (redisCache != null) {
            redisCache.put(pageKey, response);
            redisCache.put(unreadCountKey(recipientUserId), response.unreadCount());
        }
        return response;
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String recipientUserId) {
        return (redisCache == null ? java.util.Optional.<Long>empty() : redisCache.get(unreadCountKey(recipientUserId), Long.class))
                .orElseGet(() -> {
                    long count = notificationRepository.countByRecipientUserIdAndReadAtIsNull(recipientUserId);
                    if (redisCache != null) redisCache.put(unreadCountKey(recipientUserId), count);
                    return count;
                });
    }

    @Transactional
    public NotificationResponse markRead(String recipientUserId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndRecipientUserId(notificationId, recipientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
        }
        evictRecipientCache(recipientUserId);
        return toResponse(notification);
    }

    @Transactional
    public int markAllRead(String recipientUserId) {
        int updated = notificationRepository.markAllReadByRecipient(recipientUserId, Instant.now());
        evictRecipientCache(recipientUserId);
        return updated;
    }

    @Transactional
    public void delete(String recipientUserId, UUID notificationId) {
        if (notificationRepository.deleteByIdAndRecipientUserId(notificationId, recipientUserId) == 0) {
            throw new ResourceNotFoundException("Notification not found");
        }
        evictRecipientCache(recipientUserId);
    }

    private void evictRecipientCache(String recipientUserId) {
        if (redisCache != null) {
            redisCache.evict(unreadCountKey(recipientUserId));
            redisCache.evictByPattern("mentify:v1:notifications:" + recipientUserId + ":page:*");
        }
    }

    private String unreadCountKey(String recipientUserId) {
        return RedisKey.of("mentify:v1", "notifications", recipientUserId, "unread-count");
    }

    private String notificationPageKey(String recipientUserId, int page, int size, boolean unreadOnly) {
        return RedisKey.of("mentify:v1", "notifications", recipientUserId,
                "page", String.valueOf(page), "size", String.valueOf(size),
                unreadOnly ? "unread" : "all");
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getSourceService(),
                notification.getOccurredAt(),
                notification.getCreatedAt(),
                notification.getReadAt(),
                notification.getEmailStatus()
        );
    }
}
