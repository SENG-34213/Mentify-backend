package com.mentify.ai.dto.tool;

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
public class TodayQuizPerformanceToolResult {

    private LocalDate date;
    private String resultType;
    private List<QuizPerformanceSummary> quizzes;
}
