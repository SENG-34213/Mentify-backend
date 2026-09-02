package com.mentify.quiz.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionOptionRequest {

    @NotBlank(message = "Option text is required")
    private String optionText;

    @NotNull(message = "Correct answer flag is required")
    private Boolean correct;

    @NotNull(message = "Option order is required")
    private Integer optionOrder;
}
