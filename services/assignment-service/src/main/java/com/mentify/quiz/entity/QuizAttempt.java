package com.mentify.quiz.entity;

import com.mentify.entity.BaseEntity;
import com.mentify.quiz.enums.AttemptStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quiz_attempts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttempt extends BaseEntity {

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID quizId;

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID studentId;

    @Column(nullable = false)
    private Integer attemptNumber;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime submittedAt;

    @Column(precision = 10, scale = 2)
    private BigDecimal score;

    @Column(precision = 5, scale = 2)
    private BigDecimal percentage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("answeredAt ASC")
    @Builder.Default
    private List<StudentAnswer> answers = new ArrayList<>();
}
