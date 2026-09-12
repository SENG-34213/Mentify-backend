package com.mentify.attendance.entity;

import com.mentify.attendance.enums.AttendanceMode;
import com.mentify.attendance.enums.AttendanceSessionStatus;
import com.mentify.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "attendance_sessions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_attendance_session_course_date_start",
                columnNames = {"course_id", "attendance_date", "start_time"}
        ),
        indexes = @Index(name = "idx_attendance_session_course_date", columnList = "course_id, attendance_date")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSession extends BaseEntity {

    @Column(name = "course_id", nullable = false, columnDefinition = "uuid")
    private UUID courseId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AttendanceMode mode = AttendanceMode.PHYSICAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AttendanceSessionStatus status = AttendanceSessionStatus.OPEN;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AttendanceRecord> records = new ArrayList<>();
}
