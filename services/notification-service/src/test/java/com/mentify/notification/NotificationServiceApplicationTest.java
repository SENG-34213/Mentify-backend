package com.mentify.notification;

import com.mentify.common.notification.NotificationEvent;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.notification.dto.NotificationPageResponse;
import com.mentify.notification.entity.Notification;
import com.mentify.notification.repository.NotificationRepository;
import com.mentify.notification.service.NotificationIngestionService;
import com.mentify.notification.service.NotificationManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceApplicationTest {

    @Autowired
    private NotificationIngestionService ingestionService;

    @Autowired
    private NotificationManagementService managementService;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void clearNotifications() {
        notificationRepository.deleteAll();
    }

    @Test
    void eventIsPersistedOnceAndOnlyVisibleToItsRecipient() {
        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID(),
                "user-123",
                "student@example.com",
                "COURSE_CREATED",
                "Course created",
                "A course was created.",
                "course-service",
                Instant.now()
        );

        ingestionService.ingest(event);
        ingestionService.ingest(event);

        NotificationPageResponse recipientNotifications =
                managementService.getNotifications("user-123", 0, 20, false);
        NotificationPageResponse otherUserNotifications =
                managementService.getNotifications("user-456", 0, 20, false);

        assertEquals(1, recipientNotifications.totalElements());
        assertEquals(1, recipientNotifications.unreadCount());
        assertEquals(0, otherUserNotifications.totalElements());
        assertEquals(1, notificationRepository.findAll().size());
    }

    @Test
    void notificationCannotBeMarkedReadByAnotherUser() {
        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID(),
                "owner-1",
                null,
                "COURSE_CREATED",
                "Course created",
                "A course was created.",
                "course-service",
                Instant.now()
        );
        ingestionService.ingest(event);
        Notification notification = notificationRepository.findAll().get(0);

        assertThrows(
                ResourceNotFoundException.class,
                () -> managementService.markRead("other-user", notification.getId())
        );
        assertEquals(1, managementService.getUnreadCount("owner-1"));
    }

    @Test
    void notificationCanOnlyBeDeletedByItsRecipient() {
        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID(),
                "owner-2",
                null,
                "COURSE_CREATED",
                "Course created",
                "A course was created.",
                "course-service",
                Instant.now()
        );
        ingestionService.ingest(event);
        Notification notification = notificationRepository.findAll().get(0);

        assertThrows(
                ResourceNotFoundException.class,
                () -> managementService.delete("other-user", notification.getId())
        );
        assertEquals(1, notificationRepository.findAll().size());

        managementService.delete("owner-2", notification.getId());

        assertEquals(0, notificationRepository.findAll().size());
    }
}
