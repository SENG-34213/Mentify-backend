package com.mentify.quiz.dto.response;

import com.mentify.quiz.enums.AttemptStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartQuizAttemptResponse {

    private UUID attemptId;
    private UUID quizId;
    private UUID studentId;
    private Integer attemptNumber;
    private AttemptStatus status;
    private LocalDateTime startedAt;
}
