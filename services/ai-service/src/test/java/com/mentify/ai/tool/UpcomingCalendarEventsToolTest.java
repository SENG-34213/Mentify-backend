package com.mentify.ai.tool;

import com.mentify.ai.client.CalendarEventsClient;
import com.mentify.ai.dto.tool.CalendarEventSummary;
import com.mentify.payload.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpcomingCalendarEventsToolTest {

    private static final String AUTHORIZATION_HEADER = "Bearer token";

    @Mock
    private CalendarEventsClient calendarEventsClient;

    @Test
    void requestsUpcomingEventsWithUserAuthorizationAndLimit() {
        List<CalendarEventSummary> events = List.of(CalendarEventSummary.builder()
                .title("Class")
                .eventType("CLASS")
                .build());
        when(calendarEventsClient.getUpcomingEvents(20, AUTHORIZATION_HEADER))
                .thenReturn(ApiResponse.success(200, "ok", events));

        UpcomingCalendarEventsTool tool = new UpcomingCalendarEventsTool(calendarEventsClient);

        assertThat(tool.getName()).isEqualTo(UpcomingCalendarEventsTool.TOOL_NAME);
        assertThat(tool.execute(AUTHORIZATION_HEADER)).isSameAs(events);
        verify(calendarEventsClient).getUpcomingEvents(20, AUTHORIZATION_HEADER);
    }
}
