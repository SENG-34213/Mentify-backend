package com.mentify.quiz.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentQuizResponse {

    private UUID id;
    private UUID courseId;
    private String title;
    private String description;
    private Integer durationMinutes;
    private BigDecimal totalMarks;
    private BigDecimal passMark;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer maxAttempts;
    private List<StudentQuestionResponse> questions;
}
