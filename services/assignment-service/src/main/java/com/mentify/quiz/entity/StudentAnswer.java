package com.mentify.quiz.entity;

import com.mentify.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

@Entity
@Table(
        name = "student_answers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_student_answers_attempt_question",
                columnNames = {"attempt_id", "question_id"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAnswer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID questionId;

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID selectedOptionId;

    private Boolean correct;

    @Column(precision = 10, scale = 2)
    private BigDecimal awardedMarks;

    @Column(nullable = false)
    private LocalDateTime answeredAt;
}
