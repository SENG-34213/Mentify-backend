package com.mentify.assignment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.assignment.dto.request.GradeSubmissionRequest;
import com.mentify.assignment.dto.response.SubmissionResponse;
import com.mentify.assignment.enums.SubmissionStatus;
import com.mentify.assignment.service.GradingService;
import com.mentify.payload.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = GradingController.class,
        properties = "spring.cloud.config.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
class GradingControllerTest {

    private static final String AUTH_HEADER = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GradingService gradingService;

    @Test
    void gradeSubmission_whenRequestIsValid_returnsGradedSubmission() throws Exception {
        UUID submissionId = UUID.randomUUID();
        GradeSubmissionRequest request = GradeSubmissionRequest.builder()
                .marks(BigDecimal.valueOf(88.50))
                .feedback("Good work, but add more detail in the conclusion.")
                .build();

        SubmissionResponse response = SubmissionResponse.builder()
                .id(submissionId)
                .assignmentId(UUID.randomUUID())
                .studentId(UUID.randomUUID())
                .attemptNumber(1)
                .status(SubmissionStatus.GRADED)
                .marks(BigDecimal.valueOf(88.50))
                .feedback("Good work, but add more detail in the conclusion.")
                .gradedAt(LocalDateTime.now())
                .build();

        when(gradingService.gradeSubmission(eq(submissionId), any(GradeSubmissionRequest.class), eq(AUTH_HEADER)))
                .thenReturn(ApiResponse.<SubmissionResponse>builder()
                        .status(HttpStatus.OK)
                        .message("Submission graded successfully")
                        .data(response)
                        .build());

        mockMvc.perform(post("/api/v1/submissions/{submissionId}/grade", submissionId)
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Submission graded successfully"))
                .andExpect(jsonPath("$.data.status").value("GRADED"))
                .andExpect(jsonPath("$.data.marks").value(88.5));

        verify(gradingService).gradeSubmission(eq(submissionId), any(GradeSubmissionRequest.class), eq(AUTH_HEADER));
    }

    @Test
    void returnSubmission_whenSubmissionIsReturned_returnsReturnedSubmission() throws Exception {
        UUID submissionId = UUID.randomUUID();

        SubmissionResponse response = SubmissionResponse.builder()
                .id(submissionId)
                .assignmentId(UUID.randomUUID())
                .studentId(UUID.randomUUID())
                .attemptNumber(1)
                .status(SubmissionStatus.RETURNED)
                .returnedAt(LocalDateTime.now())
                .build();

        when(gradingService.returnSubmission(eq(submissionId), eq(AUTH_HEADER)))
                .thenReturn(ApiResponse.<SubmissionResponse>builder()
                        .status(HttpStatus.OK)
                        .message("Submission returned successfully")
                        .data(response)
                        .build());

        mockMvc.perform(post("/api/v1/submissions/{submissionId}/return", submissionId)
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Submission returned successfully"))
                .andExpect(jsonPath("$.data.status").value("RETURNED"));

        verify(gradingService).returnSubmission(eq(submissionId), eq(AUTH_HEADER));
    }
}
