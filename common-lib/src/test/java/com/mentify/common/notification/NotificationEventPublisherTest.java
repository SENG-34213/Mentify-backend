package com.mentify.common.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.kafka.core.KafkaTemplate;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private Executor notificationEventExecutor;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void dispatchesKafkaSendOutsideTheCallingThread() throws JsonProcessingException {
        NotificationEvent event = event();
        when(objectMapper.writeValueAsString(event)).thenReturn("payload");
        when(kafkaTemplate.send("notifications", event.recipientUserId(), "payload"))
                .thenReturn(CompletableFuture.completedFuture(null));
        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);

        publisher().publish(event);

        verify(notificationEventExecutor).execute(taskCaptor.capture());
        verifyNoInteractions(kafkaTemplate);

        taskCaptor.getValue().run();

        verify(kafkaTemplate).send("notifications", event.recipientUserId(), "payload");
    }

    @Test
    void dropsEventWithoutBlockingWhenDispatchQueueIsFull() throws JsonProcessingException {
        NotificationEvent event = event();
        when(objectMapper.writeValueAsString(event)).thenReturn("payload");
        doThrow(new RejectedExecutionException("queue full"))
                .when(notificationEventExecutor).execute(any(Runnable.class));

        assertDoesNotThrow(() -> publisher().publish(event));

        verify(kafkaTemplate, never()).send(any(String.class), any(String.class), any(String.class));
    }

    @Test
    void returnsWhileKafkaSendIsBlocked() throws JsonProcessingException, InterruptedException {
        NotificationEvent event = event();
        CountDownLatch sendStarted = new CountDownLatch(1);
        CountDownLatch releaseSend = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        when(objectMapper.writeValueAsString(event)).thenReturn("payload");
        when(kafkaTemplate.send("notifications", event.recipientUserId(), "payload")).thenAnswer(invocation -> {
            sendStarted.countDown();
            assertTrue(releaseSend.await(5, TimeUnit.SECONDS), "Kafka send should be released by the test");
            return CompletableFuture.completedFuture(null);
        });

        try {
            assertTimeoutPreemptively(
                    Duration.ofSeconds(2),
                    () -> new NotificationEventPublisher(
                            kafkaTemplate,
                            executor,
                            objectMapper,
                            "notifications",
                            "course-service"
                    ).publish(event)
            );
            assertTrue(sendStarted.await(2, TimeUnit.SECONDS), "Kafka send should start on the publisher executor");
        } finally {
            releaseSend.countDown();
            executor.shutdownNow();
        }
    }

    private NotificationEventPublisher publisher() {
        return new NotificationEventPublisher(
                kafkaTemplate,
                notificationEventExecutor,
                objectMapper,
                "notifications",
                "course-service"
        );
    }

    private NotificationEvent event() {
        return new NotificationEvent(
                UUID.randomUUID(),
                "user-123",
                "user@example.com",
                "COURSE_CREATED",
                "Course created",
                "A course was created.",
                "course-service",
                Instant.now()
        );
    }
}
