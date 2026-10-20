package com.mentify.ai.tool;

import com.mentify.ai.client.CalendarEventsClient;
import com.mentify.ai.dto.tool.CalendarEventSummary;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpcomingCalendarEventsTool implements AiTool<List<CalendarEventSummary>> {

    public static final String TOOL_NAME = "getUpcomingCalendarEvents";
    private static final int EVENT_LIMIT = 20;

    private final CalendarEventsClient calendarEventsClient;

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public List<CalendarEventSummary> execute(String authorizationHeader) {
        log.info("Executing AI tool {}", TOOL_NAME);
        ApiResponse<List<CalendarEventSummary>> response =
                calendarEventsClient.getUpcomingEvents(EVENT_LIMIT, authorizationHeader);
        return response.getData();
    }
}
