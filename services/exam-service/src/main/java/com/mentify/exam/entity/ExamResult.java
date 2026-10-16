package com.mentify.exam.entity;

import com.mentify.exam.enums.AttendanceStatus;
import com.mentify.exam.enums.ResultStatus;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** One result per exam and student. studentId is an external reference; no Student or Enrollment data is kept. */
@Entity
@Table(
        name = "exam_results",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_exam_results_exam_student", columnNames = {"exam_id", "student_id"}),
        indexes = {
                @Index(name = "idx_exam_results_exam_id", columnList = "exam_id"),
                @Index(name = "idx_exam_results_student_id", columnList = "student_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "exam_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID examId;

    @Column(name = "student_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID studentId;

    @Column(name = "marks_obtained", precision = 8, scale = 2)
    private BigDecimal marksObtained;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", length = 20)
    private AttendanceStatus attendanceStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 20)
    @Builder.Default
    private ResultStatus resultStatus = ResultStatus.NOT_MARKED;

    @Column(length = 10)
    private String grade;

    @Column(length = 1000)
    private String remarks;

    @Column(name = "marked_by", columnDefinition = "uuid")
    private UUID markedBy;

    @Column(name = "marked_at")
    private LocalDateTime markedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
