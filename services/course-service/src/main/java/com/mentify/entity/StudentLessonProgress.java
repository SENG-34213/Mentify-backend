package com.mentify.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_lesson_progress", uniqueConstraints = @UniqueConstraint(
        name = "uq_student_lesson_progress_student_lesson", columnNames = {"student_id", "lesson_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentLessonProgress extends BaseEntity {

    @Column(name = "student_id", nullable = false, columnDefinition = "uuid")
    private UUID studentId;

    @Column(name = "course_id", nullable = false, columnDefinition = "uuid")
    private UUID courseId;

    @Column(name = "lesson_id", nullable = false, columnDefinition = "uuid")
    private UUID lessonId;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "last_accessed_at", nullable = false)
    private LocalDateTime lastAccessedAt;
}
