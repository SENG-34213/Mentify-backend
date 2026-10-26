package com.mentify.ai.tool;

import com.mentify.ai.client.AdminAnalyticsClient;
import com.mentify.ai.dto.tool.AdminDashboardOverview;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardOverviewTool implements AiTool<AdminDashboardOverview> {

    public static final String TOOL_NAME = "getAdminDashboardOverview";

    private final AdminAnalyticsClient adminAnalyticsClient;
    private AiScopedTtlCache cache = new AiScopedTtlCache();

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public AdminDashboardOverview execute(String authorizationHeader) {
        log.info("Executing AI tool {}", TOOL_NAME);
        return cache.get(TOOL_NAME, authorizationHeader, Duration.ofSeconds(30),
                () -> adminAnalyticsClient.getOverview(authorizationHeader));
    }
}
