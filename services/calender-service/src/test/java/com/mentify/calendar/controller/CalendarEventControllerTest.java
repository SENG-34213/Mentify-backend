package com.mentify.calendar.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.calendar.dto.CalendarEventRequest;
import com.mentify.calendar.dto.CalendarEventResponse;
import com.mentify.calendar.enums.CalendarEventType;
import com.mentify.calendar.enums.CalendarVisibility;
import com.mentify.calendar.exception.CalendarExceptionHandler;
import com.mentify.calendar.service.CalendarEventService;
import com.mentify.payload.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CalendarEventControllerTest {

    @Mock
    private CalendarEventService service;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CalendarEventController(service))
                .setControllerAdvice(new CalendarExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void createReturnsStructuredCreatedResponse() throws Exception {
        CalendarEventRequest request = validRequest();
        UUID eventId = UUID.randomUUID();
        when(service.create(any(CalendarEventRequest.class), eq("Bearer token")))
                .thenReturn(CalendarEventResponse.builder().id(eventId).title("Meeting").build());

        mockMvc.perform(post("/api/v1/calendar/events")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.id").value(eventId.toString()));
    }

    @Test
    void invalidCreateReturnsSafeValidationEnvelope() throws Exception {
        CalendarEventRequest request = validRequest();
        request.setTitle("");

        mockMvc.perform(post("/api/v1/calendar/events")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("title is required"));
    }

    private CalendarEventRequest validRequest() {
        CalendarEventRequest request = new CalendarEventRequest();
        request.setTitle("Meeting");
        request.setEventType(CalendarEventType.MEETING);
        request.setStartDateTime(LocalDateTime.of(2026, 10, 12, 9, 0));
        request.setEndDateTime(LocalDateTime.of(2026, 10, 12, 10, 0));
        request.setAllDay(false);
        request.setVisibility(CalendarVisibility.GLOBAL);
        return request;
    }
}
