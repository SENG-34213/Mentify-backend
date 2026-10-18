package com.mentify.controller;

import com.mentify.dto.TeacherStudentDirectoryResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.TeacherStudentDirectoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TeacherStudentDirectoryController.class,
        properties = "spring.cloud.config.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
class TeacherStudentDirectoryControllerTest {

    private static final String AUTH_HEADER = "Bearer directory-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeacherStudentDirectoryService directoryService;

    @Test
    void getStudents_returnsStudentDirectoryEntries() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        TeacherStudentDirectoryResponse student = TeacherStudentDirectoryResponse.builder()
                .id(studentId)
                .name("Student Name")
                .email("student@example.com")
                .studentNumber("STU-001")
                .status("ACTIVE")
                .courses(List.of(courseId))
                .build();
        when(directoryService.getStudents(courseId, AUTH_HEADER)).thenReturn(ApiResponse.<List<TeacherStudentDirectoryResponse>>builder()
                .status(HttpStatus.OK)
                .message("Teacher students fetched successfully")
                .data(List.of(student))
                .build());

        mockMvc.perform(get("/api/v1/teacher/students")
                        .param("courseId", courseId.toString())
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(studentId.toString()))
                .andExpect(jsonPath("$.data[0].name").value("Student Name"))
                .andExpect(jsonPath("$.data[0].studentNumber").value("STU-001"))
                .andExpect(jsonPath("$.data[0].courses[0]").value(courseId.toString()));

        verify(directoryService).getStudents(courseId, AUTH_HEADER);
    }

    @Test
    void getStudents_allowsTeacherAdminAndSuperAdminRoles() throws NoSuchMethodException {
        Method method = TeacherStudentDirectoryController.class
                .getDeclaredMethod("getStudents", UUID.class, String.class);
        assertThat(method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class).value())
                .contains("TEACHER", "ADMIN", "SUPER_ADMIN");
    }
}
