package com.mentify.ai.tool;

import com.mentify.ai.client.AttendanceAnalyticsClient;
import com.mentify.ai.dto.tool.AttendanceAnalyticsSummary;
import com.mentify.payload.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendancePerformanceToolTest {

    private static final String AUTHORIZATION_HEADER = "Bearer token";

    @Mock
    private AttendanceAnalyticsClient attendanceAnalyticsClient;

    @Test
    void requestsAttendanceOverviewWithUserAuthorization() {
        AttendanceAnalyticsSummary summary = new AttendanceAnalyticsSummary(null, new BigDecimal("87.50"));
        when(attendanceAnalyticsClient.getOverview(null, null, AUTHORIZATION_HEADER))
                .thenReturn(ApiResponse.success(200, "ok", summary));

        AttendancePerformanceTool tool = new AttendancePerformanceTool(attendanceAnalyticsClient);

        assertThat(tool.getName()).isEqualTo(AttendancePerformanceTool.TOOL_NAME);
        assertThat(tool.execute(AUTHORIZATION_HEADER)).isSameAs(summary);
        verify(attendanceAnalyticsClient).getOverview(null, null, AUTHORIZATION_HEADER);
    }
}
