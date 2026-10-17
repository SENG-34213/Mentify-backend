package com.mentify.ai.tool;

import com.mentify.ai.client.ExamAnalyticsClient;
import com.mentify.ai.dto.tool.StudentExamResultsToolResult;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MyExamResultsTool implements AiTool<StudentExamResultsToolResult> {

    public static final String TOOL_NAME = "getMyExamResults";

    private final ExamAnalyticsClient examAnalyticsClient;

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public StudentExamResultsToolResult execute(String authorizationHeader) {
        log.info("Executing AI tool {}", TOOL_NAME);
        ApiResponse<StudentExamResultsToolResult> response =
                examAnalyticsClient.getMyExamResults(authorizationHeader);
        return response.getData();
    }
}
