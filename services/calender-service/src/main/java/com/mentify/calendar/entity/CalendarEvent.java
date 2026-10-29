package com.mentify.calendar.entity;

import com.mentify.calendar.enums.CalendarEventStatus;
import com.mentify.calendar.enums.CalendarEventType;
import com.mentify.calendar.enums.CalendarSourceType;
import com.mentify.calendar.enums.CalendarTargetRole;
import com.mentify.calendar.enums.CalendarVisibility;
import com.mentify.calendar.config.CalendarTimeZone;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "calendar_events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_calendar_events_source_pair",
                        columnNames = {"source_type", "source_id"}
                )
        },
        indexes = {
                @Index(name = "idx_calendar_events_start_date_time", columnList = "start_date_time"),
                @Index(name = "idx_calendar_events_end_date_time", columnList = "end_date_time"),
                @Index(name = "idx_calendar_events_course_id", columnList = "course_id"),
                @Index(name = "idx_calendar_events_created_by", columnList = "created_by"),
                @Index(name = "idx_calendar_events_event_type", columnList = "event_type"),
                @Index(name = "idx_calendar_events_status", columnList = "status"),
                @Index(name = "idx_calendar_events_visibility", columnList = "visibility")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private CalendarEventType eventType;

    @NotNull
    @Column(name = "start_date_time", nullable = false)
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time")
    private LocalDateTime endDateTime;

    @Column(name = "all_day", nullable = false)
    private boolean allDay;

    @Column(name = "course_id", columnDefinition = "uuid")
    private UUID courseId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private CalendarSourceType sourceType;

    @Column(name = "source_id", columnDefinition = "uuid")
    private UUID sourceId;

    @NotNull
    @Column(name = "created_by", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID createdBy;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CalendarVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_role", length = 30)
    private CalendarTargetRole targetRole;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CalendarEventStatus status;

    @Size(max = 255)
    @Column(length = 255)
    private String location;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @AssertTrue(message = "endDateTime must not precede startDateTime")
    public boolean isDateRangeValid() {
        return startDateTime == null || endDateTime == null || !endDateTime.isBefore(startDateTime);
    }

    @AssertTrue(message = "timed events require endDateTime")
    public boolean isTimedEventComplete() {
        return allDay || endDateTime != null;
    }

    @AssertTrue(message = "COURSE visibility requires courseId")
    public boolean isCourseVisibilityValid() {
        return visibility == null || visibility != CalendarVisibility.COURSE || courseId != null;
    }

    @AssertTrue(message = "ROLE visibility requires targetRole")
    public boolean isRoleVisibilityValid() {
        return visibility == null || visibility != CalendarVisibility.ROLE || targetRole != null;
    }

    @AssertTrue(message = "external source events require sourceId")
    public boolean isSourceReferenceValid() {
        return sourceType == null
                || sourceType == CalendarSourceType.CUSTOM
                || sourceType == CalendarSourceType.ADMIN
                || sourceId != null;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now(CalendarTimeZone.current());
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = CalendarEventStatus.SCHEDULED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(CalendarTimeZone.current());
    }
}
