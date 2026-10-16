package com.mentify.attendance.controller;

import com.mentify.attendance.dto.response.AttendanceSummaryResponse;
import com.mentify.attendance.service.AttendanceSummaryService;
import com.mentify.payload.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AttendanceSummaryController.class,
        properties = "spring.cloud.config.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
class AttendanceSummaryControllerTest {

    private static final String AUTH_HEADER = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttendanceSummaryService attendanceSummaryService;

    @Test
    void getMyAttendanceSummary_returnsSummary() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        AttendanceSummaryResponse summary = AttendanceSummaryResponse.builder()
                .studentId(studentId)
                .courseId(courseId)
                .completedSessions(10)
                .presentCount(7)
                .lateCount(1)
                .absentCount(2)
                .attendedCount(8)
                .attendancePercentage(80.0)
                .build();

        when(attendanceSummaryService.getMyAttendanceSummary(eq(courseId), eq(AUTH_HEADER)))
                .thenReturn(ApiResponse.<AttendanceSummaryResponse>builder()
                        .status(HttpStatus.OK)
                        .message("Attendance summary fetched successfully")
                        .data(summary)
                        .build());

        mockMvc.perform(get("/api/v1/attendance/courses/{courseId}/me", courseId)
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attendancePercentage").value(80.0));
    }
}
