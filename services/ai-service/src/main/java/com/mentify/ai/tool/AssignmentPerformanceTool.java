package com.mentify.ai.tool;

import com.mentify.ai.client.AssignmentAnalyticsClient;
import com.mentify.ai.dto.tool.AssignmentAnalyticsSummary;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssignmentPerformanceTool implements AiTool<AssignmentAnalyticsSummary> {

    public static final String TOOL_NAME = "getAssignmentPerformance";

    private final AssignmentAnalyticsClient assignmentAnalyticsClient;

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public AssignmentAnalyticsSummary execute(String authorizationHeader) {
        return execute(null, null, null, authorizationHeader);
    }

    public AssignmentAnalyticsSummary execute(
            UUID courseId, LocalDate from, LocalDate to, String authorizationHeader) {
        log.info("Executing AI tool {} courseId={} from={} to={}", TOOL_NAME, courseId, from, to);
        ApiResponse<AssignmentAnalyticsSummary> response = courseId == null
                ? assignmentAnalyticsClient.getOverview(from, to, authorizationHeader)
                : assignmentAnalyticsClient.getCourseSummary(courseId, from, to, authorizationHeader);
        return response == null ? null : response.getData();
    }
}
