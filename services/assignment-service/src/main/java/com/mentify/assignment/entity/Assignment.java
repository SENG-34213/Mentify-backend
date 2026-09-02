package com.mentify.assignment.entity;

import com.mentify.assignment.enums.AssignmentStatus;
import com.mentify.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @Column(nullable = false, columnDefinition = "uuid")
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

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime dueDate;

    @Column(nullable = false)
    private Integer maxMarks;

    @Column(nullable = false)
    private Integer allowedAttempts;

    @Column(nullable = false)
    @Builder.Default
    private Boolean allowLateSubmission = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer latePenaltyPercentage = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.DRAFT;

    private LocalDateTime publishedAt;

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AssignmentAttachment> attachments = new ArrayList<>();
}
