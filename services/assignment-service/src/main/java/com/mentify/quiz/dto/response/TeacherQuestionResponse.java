package com.mentify.quiz.dto.response;

import com.mentify.quiz.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherQuestionResponse {

    private UUID id;
    private String questionText;
    private QuestionType questionType;
    private BigDecimal marks;
    private Integer questionOrder;
    private List<TeacherOptionResponse> options;
}
