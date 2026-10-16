package com.mentify.notification.service;

import com.mentify.common.notification.NotificationEvent;
import com.mentify.notification.entity.EmailDeliveryStatus;
import com.mentify.notification.entity.Notification;
import com.mentify.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationIngestionServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationIngestionService service;

    @Test
    void ingestStoresNotificationAndQueuesEmailOnce() {
        NotificationEvent event = event();
        when(notificationRepository.existsByEventIdAndRecipientUserId(event.eventId(), event.recipientUserId()))
                .thenReturn(false);

        service.ingest(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification stored = captor.getValue();
        assertEquals(event.eventId(), stored.getEventId());
        assertEquals(event.recipientUserId(), stored.getRecipientUserId());
        assertEquals(event.recipientEmail(), stored.getRecipientEmail());
        assertEquals(EmailDeliveryStatus.PENDING, stored.getEmailStatus());
        assertNotNull(stored.getNextEmailAttemptAt());
    }

    @Test
    void ingestDeduplicatesRepeatedKafkaDelivery() {
        NotificationEvent event = event();
        when(notificationRepository.existsByEventIdAndRecipientUserId(event.eventId(), event.recipientUserId()))
                .thenReturn(true);

        service.ingest(event);

        verify(notificationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ingestRejectsEventsWithoutRequiredIdentity() {
        NotificationEvent invalidEvent = new NotificationEvent(
                UUID.randomUUID(),
                " ",
                null,
                "COURSE_CREATED",
                "Course created",
                "The course was created",
                "course-service",
                Instant.now()
        );

        assertThrows(IllegalArgumentException.class, () -> service.ingest(invalidEvent));
        verify(notificationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ingestDoesNotQueueEmailWhenNoEmailWasProvided() {
        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID(),
                "user-123",
                null,
                "COURSE_CREATED",
                "Course created",
                "The course was created",
                "course-service",
                Instant.now()
        );
        when(notificationRepository.existsByEventIdAndRecipientUserId(event.eventId(), event.recipientUserId()))
                .thenReturn(false);

        service.ingest(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(EmailDeliveryStatus.NOT_REQUESTED, captor.getValue().getEmailStatus());
        assertNull(captor.getValue().getNextEmailAttemptAt());
    }

    private NotificationEvent event() {
        return new NotificationEvent(
                UUID.randomUUID(),
                "user-123",
                "user@example.com",
                "COURSE_CREATED",
                "Course created",
                "The course was created",
                "course-service",
                Instant.now()
        );
    }
}
