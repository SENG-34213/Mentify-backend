package com.mentify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.dto.EnrollmentRequest;
import com.mentify.dto.EnrollmentResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.EnrollmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = EnrollmentController.class,
        properties = "spring.cloud.config.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EnrollmentService enrollmentService;

    @Test
    void createEnrollment_whenRequestIsValid_returnsCreatedEnrollment() throws Exception {
        UUID enrollmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        EnrollmentResponse enrollmentResponse = EnrollmentResponse.builder()
                .id(enrollmentId)
                .studentId(studentId)
                .courseId(courseId)
                .build();

        when(enrollmentService.createEnrollment(any(EnrollmentRequest.class)))
                .thenReturn(ApiResponse.<EnrollmentResponse>builder()
                        .status(HttpStatus.CREATED)
                        .message("Enrollment created successfully")
                        .data(enrollmentResponse)
                        .build());

        EnrollmentRequest request = EnrollmentRequest.builder()
                .studentId(studentId)
                .courseId(courseId)
                .build();

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Enrollment created successfully"))
                .andExpect(jsonPath("$.data.studentId").value(studentId.toString()))
                .andExpect(jsonPath("$.data.courseId").value(courseId.toString()));

        verify(enrollmentService).createEnrollment(any(EnrollmentRequest.class));
    }

    @Test
    void createEnrollment_whenStudentIdIsMissing_returnsBadRequest() throws Exception {
        EnrollmentRequest request = EnrollmentRequest.builder()
                .courseId(UUID.randomUUID())
                .build();

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(enrollmentService);
    }

    @Test
    void createEnrollment_hasAdminAndSuperAdminPreAuthorize() throws NoSuchMethodException {
        Method method = EnrollmentController.class.getDeclaredMethod("createEnrollment", EnrollmentRequest.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains("ADMIN");
        assertThat(preAuthorize.value()).contains("SUPER_ADMIN");
    }
}
