package com.mentify.notification.service;

import com.mentify.notification.entity.EmailDeliveryStatus;
import com.mentify.notification.entity.Notification;
import com.mentify.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import java.time.Instant;
import java.util.Properties;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEmailDeliveryServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationEmailDeliveryService service;

    private Notification notification;

    @BeforeEach
    void setUp() {
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setRecipientEmail("student@example.com");
        notification.setTitle("Exam scheduled");
        notification.setMessage("Your exam was scheduled.");
        notification.setEmailStatus(EmailDeliveryStatus.PENDING);
        notification.setEmailAttempts(0);

        ReflectionTestUtils.setField(service, "fromAddress", "no-reply@mentify.local");
        ReflectionTestUtils.setField(service, "retryDelayMillis", 1000L);
        ReflectionTestUtils.setField(service, "maxAttempts", 2);
    }

    @Test
    void deliverMarksEmailAsSentAfterSmtpAcceptsIt() {
        when(notificationRepository.findByIdForEmailDelivery(notification.getId())).thenReturn(Optional.of(notification));

        service.deliver(notification.getId());

        verify(mailSender).send(any(MimeMessage.class));
        assertEquals(EmailDeliveryStatus.SENT, notification.getEmailStatus());
        assertEquals(1, notification.getEmailAttempts());
        assertNull(notification.getRecipientEmail());
        assertNull(notification.getNextEmailAttemptAt());
    }

    @Test
    void deliverSchedulesRetryAfterTransientSmtpFailure() {
        when(notificationRepository.findByIdForEmailDelivery(notification.getId())).thenReturn(Optional.of(notification));
        doThrow(new MailSendException("SMTP unavailable"))
                .when(mailSender).send(any(MimeMessage.class));

        service.deliver(notification.getId());

        assertEquals(EmailDeliveryStatus.PENDING, notification.getEmailStatus());
        assertEquals(1, notification.getEmailAttempts());
        assertEquals("SMTP unavailable", notification.getLastEmailError());
        assertTrue(notification.getNextEmailAttemptAt().isAfter(Instant.now()));
    }

    @Test
    void deliverStopsRetryingWhenMaximumAttemptsAreReached() {
        notification.setEmailAttempts(1);
        when(notificationRepository.findByIdForEmailDelivery(notification.getId())).thenReturn(Optional.of(notification));
        doThrow(new MailSendException("SMTP unavailable"))
                .when(mailSender).send(any(MimeMessage.class));

        service.deliver(notification.getId());

        assertEquals(EmailDeliveryStatus.FAILED, notification.getEmailStatus());
        assertEquals(2, notification.getEmailAttempts());
        assertNull(notification.getRecipientEmail());
        assertNull(notification.getNextEmailAttemptAt());
    }
}
