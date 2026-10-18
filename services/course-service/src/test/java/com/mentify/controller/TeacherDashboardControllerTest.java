package com.mentify.controller;

import com.mentify.dto.TeacherDashboardResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.CourseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TeacherDashboardController.class,
        properties = "spring.cloud.config.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
class TeacherDashboardControllerTest {

    private static final String AUTH_HEADER = "Bearer dashboard-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseService courseService;

    @Test
    void getDashboard_returnsAggregatedDashboard() throws Exception {
        TeacherDashboardResponse dashboard = TeacherDashboardResponse.builder()
                .courseCount(2)
                .studentCount(12)
                .activeAssignmentCount(3)
                .pendingReviewCount(1)
                .upcomingExamCount(2)
                .todayClassCount(1)
                .todaySchedule(List.of())
                .pendingGrading(List.of())
                .coursePerformance(List.of())
                .build();
        when(courseService.getTeacherDashboard(AUTH_HEADER)).thenReturn(ApiResponse.<TeacherDashboardResponse>builder()
                .status(HttpStatus.OK)
                .message("Teacher dashboard fetched successfully")
                .data(dashboard)
                .build());

        mockMvc.perform(get("/api/v1/teacher/dashboard").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseCount").value(2))
                .andExpect(jsonPath("$.data.studentCount").value(12))
                .andExpect(jsonPath("$.data.pendingReviewCount").value(1))
                .andExpect(jsonPath("$.data.todaySchedule").isArray())
                .andExpect(jsonPath("$.data.pendingGrading").isArray())
                .andExpect(jsonPath("$.data.coursePerformance").isArray());

        verify(courseService).getTeacherDashboard(eq(AUTH_HEADER));
    }

    @Test
    void getDashboard_allowsTeacherAdminAndSuperAdminRoles() throws NoSuchMethodException {
        Method method = TeacherDashboardController.class.getDeclaredMethod("getDashboard", String.class);
        assertThat(method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class).value())
                .contains("TEACHER", "ADMIN", "SUPER_ADMIN");
    }
}
