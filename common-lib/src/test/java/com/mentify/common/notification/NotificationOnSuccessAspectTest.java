package com.mentify.common.notification;

import com.mentify.payload.response.ApiResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationOnSuccessAspectTest {

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private NotifyOnSuccess notification;

    @InjectMocks
    private NotificationOnSuccessAspect aspect;

    @Test
    void publishesOnlyAfterSuccessfulHttpResponse() throws Throwable {
        when(notification.type()).thenReturn("COURSE_CREATED");
        when(notification.title()).thenReturn("Course created");
        when(notification.message()).thenReturn("Your course was created.");
        ResponseEntity<String> response = ResponseEntity.ok("created");
        when(joinPoint.proceed()).thenReturn(response);

        assertSame(response, aspect.publishAfterSuccessfulResponse(joinPoint, notification));

        verify(notificationEventPublisher).publishForCurrentUser(
                "COURSE_CREATED",
                "Course created",
                "Your course was created."
        );
    }

    @Test
    void doesNotPublishWhenApiResponseIsAnError() throws Throwable {
        ApiResponse<Object> response = ApiResponse.builder()
                .statusCode(HttpStatus.CONFLICT.value())
                .message("Conflict")
                .build();
        when(joinPoint.proceed()).thenReturn(response);

        aspect.publishAfterSuccessfulResponse(joinPoint, notification);

        verify(notificationEventPublisher, never()).publishForCurrentUser(
                "COURSE_CREATED",
                "Course created",
                "Your course was created."
        );
    }

    @Test
    void doesNotPublishWhenHandlerThrows() throws Throwable {
        IllegalStateException exception = new IllegalStateException("Failed");
        when(joinPoint.proceed()).thenThrow(exception);

        assertThrows(
                IllegalStateException.class,
                () -> aspect.publishAfterSuccessfulResponse(joinPoint, notification)
        );

        verify(notificationEventPublisher, never()).publishForCurrentUser(
                "COURSE_CREATED",
                "Course created",
                "Your course was created."
        );
    }
}
