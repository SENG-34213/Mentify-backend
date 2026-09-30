package com.mentify.ai.dto.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizPerformanceSummary {

    private UUID quizId;
    private UUID courseId;
    private String quizTitle;
    private LocalDateTime startTime;
    private BigDecimal totalMarks;
    private BigDecimal passMark;
    private long submittedAttempts;
    private long participants;
    private BigDecimal averagePercentage;
    private BigDecimal highestPercentage;
    private BigDecimal lowestPercentage;
    private long passedAttempts;
    private long belowPassThresholdAttempts;
}
