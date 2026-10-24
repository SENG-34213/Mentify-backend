package com.mentify.ai.tool;

import com.mentify.ai.client.AssignmentAnalyticsClient;
import com.mentify.ai.dto.tool.AssignmentAnalyticsSummary;
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
class AssignmentPerformanceToolTest {

    private static final String AUTHORIZATION_HEADER = "Bearer token";

    @Mock
    private AssignmentAnalyticsClient assignmentAnalyticsClient;

    @Test
    void requestsCourseAssignmentAnalyticsWithDateRangeAndAuthorization() {
        UUID courseId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);
        AssignmentAnalyticsSummary summary = new AssignmentAnalyticsSummary(
                courseId, new BigDecimal("80.00"), new BigDecimal("75.00"), new BigDecimal("20.00"));
        when(assignmentAnalyticsClient.getCourseSummary(courseId, from, to, AUTHORIZATION_HEADER))
                .thenReturn(ApiResponse.success(200, "ok", summary));

        AssignmentPerformanceTool tool = new AssignmentPerformanceTool(assignmentAnalyticsClient);

        assertThat(tool.execute(courseId, from, to, AUTHORIZATION_HEADER)).isSameAs(summary);
        verify(assignmentAnalyticsClient).getCourseSummary(courseId, from, to, AUTHORIZATION_HEADER);
    }
}
