package com.mentify.notification.service;

import com.mentify.exception.ResourceNotFoundException;
import com.mentify.notification.dto.NotificationPageResponse;
import com.mentify.notification.dto.NotificationResponse;
import com.mentify.notification.entity.Notification;
import com.mentify.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationManagementService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public NotificationPageResponse getNotifications(String recipientUserId, int page, int size, boolean unreadOnly) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Notification> notifications = unreadOnly
                ? notificationRepository.findByRecipientUserIdAndReadAtIsNullOrderByCreatedAtDesc(
                        recipientUserId,
                        pageable
                )
                : notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(recipientUserId, pageable);

        return new NotificationPageResponse(
                notifications.map(this::toResponse).getContent(),
                notifications.getNumber(),
                notifications.getSize(),
                notifications.getTotalElements(),
                notifications.getTotalPages(),
                notificationRepository.countByRecipientUserIdAndReadAtIsNull(recipientUserId)
        );
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String recipientUserId) {
        return notificationRepository.countByRecipientUserIdAndReadAtIsNull(recipientUserId);
    }

    @Transactional
    public NotificationResponse markRead(String recipientUserId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndRecipientUserId(notificationId, recipientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
        }
        return toResponse(notification);
    }

    @Transactional
    public int markAllRead(String recipientUserId) {
        return notificationRepository.markAllReadByRecipient(recipientUserId, Instant.now());
    }

    @Transactional
    public void delete(String recipientUserId, UUID notificationId) {
        if (notificationRepository.deleteByIdAndRecipientUserId(notificationId, recipientUserId) == 0) {
            throw new ResourceNotFoundException("Notification not found");
        }
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
