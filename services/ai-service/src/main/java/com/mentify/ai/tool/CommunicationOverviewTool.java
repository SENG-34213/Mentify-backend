package com.mentify.ai.tool;

import com.mentify.ai.client.CommunicationAnalyticsClient;
import com.mentify.ai.dto.tool.CommunicationAnalyticsOverview;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommunicationOverviewTool implements AiTool<CommunicationAnalyticsOverview> {

    public static final String TOOL_NAME = "getCommunicationOverview";

    private final CommunicationAnalyticsClient communicationAnalyticsClient;
    private AiScopedTtlCache cache = new AiScopedTtlCache();

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public CommunicationAnalyticsOverview execute(String authorizationHeader) {
        log.info("Executing AI tool {}", TOOL_NAME);
        return cache.get(TOOL_NAME, authorizationHeader, Duration.ofSeconds(15), () -> {
            ApiResponse<CommunicationAnalyticsOverview> response =
                    communicationAnalyticsClient.getOverview(authorizationHeader);
            return response == null ? null : response.getData();
        });
    }
}
