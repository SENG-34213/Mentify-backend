package com.mentify.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications", uniqueConstraints = {
        @UniqueConstraint(name = "uq_notifications_event_recipient", columnNames = {"event_id", "recipient_user_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "recipient_user_id", nullable = false, length = 255)
    private String recipientUserId;

    @Column(name = "recipient_email", length = 320)
    private String recipientEmail;

    @Column(nullable = false, length = 80)
    private String type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 4000)
    private String message;

    @Column(name = "source_service", nullable = false, length = 100)
    private String sourceService;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_status", nullable = false, length = 20)
    private EmailDeliveryStatus emailStatus;

    @Column(name = "email_attempts", nullable = false)
    private int emailAttempts;

    @Column(name = "next_email_attempt_at")
    private Instant nextEmailAttemptAt;

    @Column(name = "last_email_error", length = 1000)
    private String lastEmailError;
}
