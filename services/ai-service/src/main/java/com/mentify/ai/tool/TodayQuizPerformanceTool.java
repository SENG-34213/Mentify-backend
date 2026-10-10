package com.mentify.ai.tool;

import com.mentify.ai.client.QuizAnalyticsClient;
import com.mentify.ai.dto.tool.TodayQuizPerformanceToolResult;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TodayQuizPerformanceTool implements AiTool<TodayQuizPerformanceToolResult> {

    public static final String TOOL_NAME = "getTodayQuizPerformance";

    private final QuizAnalyticsClient quizAnalyticsClient;

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public TodayQuizPerformanceToolResult execute(String authorizationHeader) {
        log.info("Executing AI tool {}", TOOL_NAME);
        ApiResponse<TodayQuizPerformanceToolResult> response =
                quizAnalyticsClient.getTodayQuizPerformance(authorizationHeader);
        return response.getData();
    }
}
