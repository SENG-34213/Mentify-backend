package com.mentify.quiz.dto.response;

import com.mentify.quiz.enums.QuizStatus;
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
public class QuizResponse {

    private UUID id;
    private UUID courseId;
    private UUID teacherId;
    private String title;
    private String description;
    private Integer durationMinutes;
    private BigDecimal totalMarks;
    private BigDecimal passMark;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer maxAttempts;
    private QuizStatus status;
    private Boolean showResultImmediately;
    private List<TeacherQuestionResponse> questions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
