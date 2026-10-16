package com.mentify.quiz.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveStudentAnswerRequest {

    @NotNull(message = "Question ID is required")
    private UUID questionId;

    @NotNull(message = "Selected option ID is required")
    private UUID selectedOptionId;
}
