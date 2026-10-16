package com.mentify.quiz.dto.response;

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
public class StudentAnswerResponse {

    private UUID id;
    private UUID attemptId;
    private UUID questionId;
    private UUID selectedOptionId;
    private LocalDateTime answeredAt;
}
