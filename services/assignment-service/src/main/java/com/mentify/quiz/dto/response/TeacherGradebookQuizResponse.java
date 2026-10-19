package com.mentify.quiz.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class TeacherGradebookQuizResponse {
    UUID quizId;
    String title;
    BigDecimal totalMarks;
    List<TeacherGradebookQuizResultResponse> results;
}
