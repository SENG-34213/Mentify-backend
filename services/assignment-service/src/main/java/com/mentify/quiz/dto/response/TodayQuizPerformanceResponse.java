package com.mentify.quiz.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayQuizPerformanceResponse {

    private LocalDate date;
    private String resultType;
    private List<QuizPerformanceSummaryResponse> quizzes;
}
