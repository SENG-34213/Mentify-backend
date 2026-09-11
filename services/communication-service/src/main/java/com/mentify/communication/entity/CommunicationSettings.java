package com.mentify.communication.entity;

import com.mentify.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "communication_settings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_communication_settings_user", columnNames = "user_id")
        },
        indexes = {
                @Index(name = "idx_communication_settings_user", columnList = "user_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunicationSettings extends BaseEntity {

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "read_receipts_enabled", nullable = false)
    @Builder.Default
    private boolean readReceiptsEnabled = true;

    @Column(name = "last_seen_visible", nullable = false)
    @Builder.Default
    private boolean lastSeenVisible = true;

    @Column(name = "profile_photo_visible", nullable = false)
    @Builder.Default
    private boolean profilePhotoVisible = true;

    @Column(name = "group_notifications_enabled", nullable = false)
    @Builder.Default
    private boolean groupNotificationsEnabled = true;

    @Column(name = "direct_notifications_enabled", nullable = false)
    @Builder.Default
    private boolean directNotificationsEnabled = true;
}
