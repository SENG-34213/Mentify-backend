package com.mentify.ai.tool;

import com.mentify.ai.client.QuizSummaryAnalyticsClient;
import com.mentify.ai.dto.tool.QuizAnalyticsSummary;
import com.mentify.payload.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizPerformanceToolTest {

    private static final String AUTHORIZATION_HEADER = "Bearer token";

    @Mock
    private QuizSummaryAnalyticsClient quizSummaryAnalyticsClient;

    @Test
    void requestsCourseQuizAnalyticsWithDateRangeAndAuthorization() {
        UUID courseId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);
        QuizAnalyticsSummary summary = new QuizAnalyticsSummary(
                courseId, new BigDecimal("90.00"), new BigDecimal("82.00"), new BigDecimal("10.00"));
        when(quizSummaryAnalyticsClient.getCourseSummary(courseId, from, to, AUTHORIZATION_HEADER))
                .thenReturn(ApiResponse.success(200, "ok", summary));

        QuizPerformanceTool tool = new QuizPerformanceTool(quizSummaryAnalyticsClient);

        assertThat(tool.execute(courseId, from, to, AUTHORIZATION_HEADER)).isSameAs(summary);
        verify(quizSummaryAnalyticsClient).getCourseSummary(courseId, from, to, AUTHORIZATION_HEADER);
    }
}
