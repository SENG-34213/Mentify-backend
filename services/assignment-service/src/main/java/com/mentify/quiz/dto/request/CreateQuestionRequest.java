package com.mentify.quiz.dto.request;

import com.mentify.quiz.enums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuestionRequest {

    @NotBlank(message = "Question text is required")
    private String questionText;

    @NotNull(message = "Question type is required")
    private QuestionType questionType;

    @NotNull(message = "Question marks are required")
    @DecimalMin(value = "0.01", message = "Question marks must be greater than zero")
    private BigDecimal marks;

    @NotNull(message = "Question order is required")
    private Integer questionOrder;

    @Valid
    @NotEmpty(message = "Question options are required")
    @Size(min = 4, max = 5, message = "A question must contain between 4 and 5 options")
    private List<QuestionOptionRequest> options;
}
