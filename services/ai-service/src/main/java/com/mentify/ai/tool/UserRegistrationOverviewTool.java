package com.mentify.ai.tool;

import com.mentify.ai.client.UserAnalyticsClient;
import com.mentify.ai.dto.tool.UserRegistrationOverviewToolResult;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationOverviewTool implements AiTool<UserRegistrationOverviewToolResult> {

    public static final String TOOL_NAME = "getUserRegistrationOverview";

    private final UserAnalyticsClient userAnalyticsClient;

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public UserRegistrationOverviewToolResult execute(String authorizationHeader) {
        log.info("Executing AI tool {}", TOOL_NAME);
        ApiResponse<UserRegistrationOverviewToolResult> response =
                userAnalyticsClient.getRegistrationOverview(authorizationHeader);
        return response.getData();
    }
}
