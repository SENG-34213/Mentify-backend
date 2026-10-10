package com.mentify.attendance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.attendance.dto.request.CreateAttendanceSessionRequest;
import com.mentify.attendance.dto.response.AttendanceSessionResponse;
import com.mentify.attendance.enums.AttendanceMode;
import com.mentify.attendance.enums.AttendanceSessionStatus;
import com.mentify.attendance.service.AttendanceSessionService;
import com.mentify.payload.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AttendanceSessionController.class,
        properties = "spring.cloud.config.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
class AttendanceSessionControllerTest {

    private static final String AUTH_HEADER = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AttendanceSessionService attendanceSessionService;

    @Test
    void createSession_whenRequestIsValid_returnsCreatedSession() throws Exception {
        UUID courseId = UUID.randomUUID();
        CreateAttendanceSessionRequest request = CreateAttendanceSessionRequest.builder()
                .courseId(courseId)
                .title("Java Programming - Week 04")
                .attendanceDate(LocalDate.now())
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .build();

        AttendanceSessionResponse sessionResponse = AttendanceSessionResponse.builder()
                .id(UUID.randomUUID())
                .courseId(courseId)
                .title("Java Programming - Week 04")
                .mode(AttendanceMode.PHYSICAL)
                .status(AttendanceSessionStatus.OPEN)
                .build();

        when(attendanceSessionService.createSession(any(CreateAttendanceSessionRequest.class), eq(AUTH_HEADER)))
                .thenReturn(ApiResponse.<AttendanceSessionResponse>builder()
                        .status(HttpStatus.CREATED)
                        .message("Attendance session created successfully")
                        .data(sessionResponse)
                        .build());

        mockMvc.perform(post("/api/v1/attendance/sessions")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Attendance session created successfully"))
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    @Test
    void createSession_whenTitleBlank_returnsBadRequest() throws Exception {
        CreateAttendanceSessionRequest request = CreateAttendanceSessionRequest.builder()
                .courseId(UUID.randomUUID())
                .title(" ")
                .attendanceDate(LocalDate.now())
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .build();

        mockMvc.perform(post("/api/v1/attendance/sessions")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
