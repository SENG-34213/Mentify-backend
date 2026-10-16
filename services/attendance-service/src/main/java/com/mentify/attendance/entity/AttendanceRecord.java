package com.mentify.attendance.entity;

import com.mentify.attendance.enums.AttendanceStatus;
import com.mentify.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "attendance_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_attendance_record_session_student",
                columnNames = {"attendance_session_id", "student_id"}
        ),
        indexes = {
                @Index(name = "idx_attendance_record_session_student", columnList = "attendance_session_id, student_id"),
                @Index(name = "idx_attendance_record_student", columnList = "student_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attendance_session_id", nullable = false)
    private AttendanceSession session;

    @Column(name = "student_id", nullable = false, columnDefinition = "uuid")
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AttendanceStatus status = AttendanceStatus.NOT_MARKED;

    @Column(length = 500)
    private String remarks;

    @Column(name = "marked_by", columnDefinition = "uuid")
    private UUID markedBy;

    @Column(name = "marked_at")
    private LocalDateTime markedAt;
}
