package com.mentify.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "announcement_reads", uniqueConstraints = {
        @UniqueConstraint(name = "uq_announcement_reads_announcement_user", columnNames = {"announcement_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class AnnouncementRead {

    @Id
    private UUID id;

    @Column(name = "announcement_id", nullable = false)
    private UUID announcementId;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "read_at", nullable = false)
    private Instant readAt;
}
