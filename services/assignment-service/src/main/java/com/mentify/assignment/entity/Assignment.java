package com.mentify.assignment.entity;

import com.mentify.assignment.enums.AssignmentStatus;
import com.mentify.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "assignments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Assignment extends BaseEntity {

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID courseId;

    @Column(columnDefinition = "uuid")
    private UUID moduleId;

    @Column(columnDefinition = "uuid")
    private UUID lessonId;

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID teacherId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime dueDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal maxMarks;

    @Column(nullable = false)
    private Integer allowedAttempts;

    @Column(nullable = false)
    @Builder.Default
    private Boolean lateSubmissionAllowed = Boolean.FALSE;

    @Column(precision = 5, scale = 2)
    private BigDecimal latePenaltyPercentage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.DRAFT;
}
