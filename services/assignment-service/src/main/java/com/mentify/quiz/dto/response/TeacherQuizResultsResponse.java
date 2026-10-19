package com.mentify.quiz.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class TeacherQuizResultsResponse {
    UUID quizId;
    BigDecimal totalMarks;
    List<TeacherQuizResultResponse> results;

    @Value
    @Builder
    public static class TeacherQuizResultResponse {
        UUID studentId;
        String studentName;
        BigDecimal marksObtained;
        BigDecimal percentage;
        boolean passed;
        LocalDateTime submittedAt;
    }
}
