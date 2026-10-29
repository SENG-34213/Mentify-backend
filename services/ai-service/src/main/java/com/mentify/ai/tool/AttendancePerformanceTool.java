package com.mentify.ai.tool;

import com.mentify.ai.client.AttendanceAnalyticsClient;
import com.mentify.ai.dto.tool.AttendanceAnalyticsSummary;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AttendancePerformanceTool implements AiTool<AttendanceAnalyticsSummary> {

    public static final String TOOL_NAME = "getAttendancePerformance";

    private final AttendanceAnalyticsClient attendanceAnalyticsClient;

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public AttendanceAnalyticsSummary execute(String authorizationHeader) {
        return execute(null, null, null, authorizationHeader);
    }

    public AttendanceAnalyticsSummary execute(UUID courseId, LocalDate from, LocalDate to, String authorizationHeader) {
        log.info("Executing AI tool {}", TOOL_NAME);
        ApiResponse<AttendanceAnalyticsSummary> response = courseId == null
                ? attendanceAnalyticsClient.getOverview(from, to, authorizationHeader)
                : attendanceAnalyticsClient.getCourseSummary(courseId, from, to, authorizationHeader);
        return response == null ? null : response.getData();
    }

    public AttendanceAnalyticsSummary executeForCourse(UUID courseId, String authorizationHeader) {
        return execute(courseId, null, null, authorizationHeader);
    }
}
