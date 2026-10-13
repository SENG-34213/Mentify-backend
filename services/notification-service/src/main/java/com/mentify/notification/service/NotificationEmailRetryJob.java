package com.mentify.notification.service;

import com.mentify.notification.entity.EmailDeliveryStatus;
import com.mentify.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mentify.notifications.email", name = "retry-enabled", havingValue = "true", matchIfMissing = true)
public class NotificationEmailRetryJob {

    private final NotificationRepository notificationRepository;
    private final NotificationEmailDeliveryService emailDeliveryService;

    @Scheduled(fixedDelayString = "${mentify.notifications.email.retry-delay-millis:30000}")
    public void retryPendingEmails() {
        notificationRepository.findTop50ByEmailStatusAndNextEmailAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        EmailDeliveryStatus.PENDING,
                        Instant.now()
                )
                .forEach(notification -> emailDeliveryService.deliver(notification.getId()));
    }
}
