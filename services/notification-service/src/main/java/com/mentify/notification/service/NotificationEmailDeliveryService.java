package com.mentify.notification.service;

import com.mentify.notification.entity.EmailDeliveryStatus;
import com.mentify.notification.entity.Notification;
import com.mentify.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
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

        try {
            MimeMessage mail = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mail, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(notification.getRecipientEmail());
            helper.setSubject("[Mentify] " + notification.getTitle());
            helper.setText(plainText(notification), htmlText(notification));
            mailSender.send(mail);
            notification.setEmailStatus(EmailDeliveryStatus.SENT);
            notification.setEmailAttempts(notification.getEmailAttempts() + 1);
            notification.setRecipientEmail(null);
            notification.setLastEmailError(null);
            notification.setNextEmailAttemptAt(null);
        } catch (MailException exception) {
            recordFailure(notification, notificationId, exception);
        } catch (MessagingException exception) {
            recordFailure(notification, notificationId,
                    new MailPreparationException("Could not prepare notification email", exception));
        }
    }

    private String plainText(Notification notification) {
        return notification.getTitle() + "\n\n" + notification.getMessage()
                + "\n\nYou received this email because email notifications are enabled for your Mentify account.";
    }

    private String htmlText(Notification notification) {
        String safeTitle = HtmlUtils.htmlEscape(notification.getTitle());
        String safeMessage = HtmlUtils.htmlEscape(notification.getMessage())
                .replace("\r\n", "\n").replace('\r', '\n').replace("\n", "<br>");
        return """
                <!doctype html>
                <html lang="en">
                  <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>%s</title></head>
                  <body style="margin:0;padding:24px;background:#f3f4f6;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:600px;margin:0 auto;background:#ffffff;border-radius:12px;">
                      <tr><td style="padding:28px 32px 12px;font-size:13px;font-weight:bold;letter-spacing:1px;color:#4f46e5;">MENTIFY</td></tr>
                      <tr><td style="padding:8px 32px 0;font-size:24px;line-height:1.3;font-weight:bold;">%s</td></tr>
                      <tr><td style="padding:16px 32px 32px;font-size:16px;line-height:1.6;">%s</td></tr>
                    </table>
                    <p style="max-width:600px;margin:16px auto 0;text-align:center;font-size:12px;line-height:1.5;color:#6b7280;">You received this email because email notifications are enabled for your Mentify account.</p>
                  </body>
                </html>
                """.formatted(safeTitle, safeTitle, safeMessage);
    }

    private void recordFailure(Notification notification, UUID notificationId, MailException exception) {
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

    private String errorSummary(MailException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return message.substring(0, Math.min(message.length(), 1000));
    }
}
