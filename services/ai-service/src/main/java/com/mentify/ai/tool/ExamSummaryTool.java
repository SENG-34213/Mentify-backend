package com.mentify.ai.tool;

import com.mentify.ai.client.ExamAnalyticsClient;
import com.mentify.ai.dto.tool.ExamSummaryToolResult;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExamSummaryTool {

    public static final String TOOL_NAME = "getExamSummary";

    private final ExamAnalyticsClient examAnalyticsClient;

    public String getName() {
        return TOOL_NAME;
    }

    public ExamSummaryToolResult execute(UUID examId, String authorizationHeader) {
        log.info("Executing AI tool {}", TOOL_NAME);
        ApiResponse<ExamSummaryToolResult> response =
                examAnalyticsClient.getExamSummary(examId, authorizationHeader);
        return response.getData();
    }
}
