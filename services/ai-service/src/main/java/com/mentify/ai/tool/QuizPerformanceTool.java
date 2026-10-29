package com.mentify.ai.tool;

import com.mentify.ai.client.QuizSummaryAnalyticsClient;
import com.mentify.ai.dto.tool.QuizAnalyticsSummary;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuizPerformanceTool implements AiTool<QuizAnalyticsSummary> {

    public static final String TOOL_NAME = "getQuizPerformance";

    private final QuizSummaryAnalyticsClient quizSummaryAnalyticsClient;

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public QuizAnalyticsSummary execute(String authorizationHeader) {
        return execute(null, null, null, authorizationHeader);
    }

    public QuizAnalyticsSummary execute(
            UUID courseId, LocalDate from, LocalDate to, String authorizationHeader) {
        log.info("Executing AI tool {} courseId={} from={} to={}", TOOL_NAME, courseId, from, to);
        ApiResponse<QuizAnalyticsSummary> response = courseId == null
                ? quizSummaryAnalyticsClient.getOverview(from, to, authorizationHeader)
                : quizSummaryAnalyticsClient.getCourseSummary(courseId, from, to, authorizationHeader);
        return response == null ? null : response.getData();
    }
}
