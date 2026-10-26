package com.mentify.ai.tool;

import com.mentify.ai.client.NotificationAnalyticsClient;
import com.mentify.ai.dto.tool.NotificationAnalyticsOverview;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationOverviewTool implements AiTool<NotificationAnalyticsOverview> {

    public static final String TOOL_NAME = "getNotificationOverview";

    private final NotificationAnalyticsClient notificationAnalyticsClient;
    private AiScopedTtlCache cache = new AiScopedTtlCache();

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public NotificationAnalyticsOverview execute(String authorizationHeader) {
        log.info("Executing AI tool {}", TOOL_NAME);
        return cache.get(TOOL_NAME, authorizationHeader, Duration.ofSeconds(15), () -> {
            ApiResponse<NotificationAnalyticsOverview> response =
                    notificationAnalyticsClient.getOverview(authorizationHeader);
            return response == null ? null : response.getData();
        });
    }
}
