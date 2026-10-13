package com.mentify.notification.service;

import com.mentify.notification.entity.EmailDeliveryStatus;
import com.mentify.notification.entity.Notification;
import com.mentify.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEmailDeliveryService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    @Value("${mentify.notifications.email.from:no-reply@mentify.local}")
    private String fromAddress;

    @Value("${mentify.notifications.email.retry-delay-millis:30000}")
    private long retryDelayMillis;

    @Value("${mentify.notifications.email.max-attempts:5}")
    private int maxAttempts;

    @Transactional
    public void deliver(UUID notificationId) {
        Notification notification = notificationRepository.findByIdForEmailDelivery(notificationId).orElse(null);
        if (notification == null || notification.getEmailStatus() != EmailDeliveryStatus.PENDING) {
            return;
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(fromAddress);
        mail.setTo(notification.getRecipientEmail());
        mail.setSubject("[Mentify] " + notification.getTitle());
        mail.setText(notification.getMessage());

        try {
            mailSender.send(mail);
            notification.setEmailStatus(EmailDeliveryStatus.SENT);
            notification.setEmailAttempts(notification.getEmailAttempts() + 1);
            notification.setRecipientEmail(null);
            notification.setLastEmailError(null);
            notification.setNextEmailAttemptAt(null);
        } catch (MailException exception) {
            int attempts = notification.getEmailAttempts() + 1;
            notification.setEmailAttempts(attempts);
            notification.setLastEmailError(errorSummary(exception));
            if (attempts >= maxAttempts) {
                notification.setEmailStatus(EmailDeliveryStatus.FAILED);
                notification.setRecipientEmail(null);
                notification.setNextEmailAttemptAt(null);
            } else {
                notification.setNextEmailAttemptAt(Instant.now().plusMillis(retryDelayMillis));
            }
            log.error("Email delivery failed for notification {} (attempt {}/{})",
                    notificationId, attempts, maxAttempts, exception);
        }
    }

    private String errorSummary(MailException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return message.substring(0, Math.min(message.length(), 1000));
    }
}
