package com.mentify.assignment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.assignment.dto.request.CreateSubmissionRequest;
import com.mentify.assignment.dto.request.UpdateSubmissionRequest;
import com.mentify.assignment.dto.response.SubmissionResponse;
import com.mentify.assignment.enums.SubmissionStatus;
import com.mentify.assignment.service.SubmissionService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = SubmissionController.class,
        properties = "spring.cloud.config.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
class SubmissionControllerTest {

    private static final String AUTH_HEADER = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubmissionService submissionService;

    @Test
    void createSubmission_whenRequestIsValid_returnsCreatedSubmission() throws Exception {
        UUID assignmentId = UUID.randomUUID();
        CreateSubmissionRequest request = CreateSubmissionRequest.builder()
                .content("This is my assignment submission.")
                .build();

        SubmissionResponse response = SubmissionResponse.builder()
                .id(UUID.randomUUID())
                .assignmentId(assignmentId)
                .studentId(UUID.randomUUID())
                .attemptNumber(1)
                .status(SubmissionStatus.DRAFT)
                .content("This is my assignment submission.")
                .isLate(false)
                .build();

        when(submissionService.createSubmission(eq(assignmentId), any(CreateSubmissionRequest.class), eq(AUTH_HEADER)))
                .thenReturn(ApiResponse.<SubmissionResponse>builder()
                        .status(HttpStatus.CREATED)
                        .message("Submission created successfully")
                        .data(response)
                        .build());

        mockMvc.perform(post("/api/v1/assignments/{assignmentId}/submissions", assignmentId)
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Submission created successfully"))
                .andExpect(jsonPath("$.data.attemptNumber").value(1))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        verify(submissionService).createSubmission(eq(assignmentId), any(CreateSubmissionRequest.class), eq(AUTH_HEADER));
    }

    @Test
    void updateSubmission_whenRequestIsValid_returnsUpdatedSubmission() throws Exception {
        UUID submissionId = UUID.randomUUID();
        UpdateSubmissionRequest request = UpdateSubmissionRequest.builder()
                .content("Updated submission content.")
                .build();

        SubmissionResponse response = SubmissionResponse.builder()
                .id(submissionId)
                .assignmentId(UUID.randomUUID())
                .studentId(UUID.randomUUID())
                .attemptNumber(1)
                .status(SubmissionStatus.DRAFT)
                .content("Updated submission content.")
                .build();

        when(submissionService.updateSubmission(eq(submissionId), any(UpdateSubmissionRequest.class), eq(AUTH_HEADER)))
                .thenReturn(ApiResponse.<SubmissionResponse>builder()
                        .status(HttpStatus.OK)
                        .message("Submission updated successfully")
                        .data(response)
                        .build());

        mockMvc.perform(put("/api/v1/submissions/{submissionId}", submissionId)
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Submission updated successfully"))
                .andExpect(jsonPath("$.data.content").value("Updated submission content."));

        verify(submissionService).updateSubmission(eq(submissionId), any(UpdateSubmissionRequest.class), eq(AUTH_HEADER));
    }

    @Test
    void listMySubmissions_whenStudentHasSubmissions_returnsList() throws Exception {
        SubmissionResponse first = SubmissionResponse.builder()
                .id(UUID.randomUUID())
                .assignmentId(UUID.randomUUID())
                .studentId(UUID.randomUUID())
                .attemptNumber(1)
                .status(SubmissionStatus.SUBMITTED)
                .content("Submission 1")
                .isLate(false)
                .submittedAt(LocalDateTime.now())
                .marks(BigDecimal.valueOf(88.00))
                .build();

        SubmissionResponse second = SubmissionResponse.builder()
                .id(UUID.randomUUID())
                .assignmentId(UUID.randomUUID())
                .studentId(UUID.randomUUID())
                .attemptNumber(2)
                .status(SubmissionStatus.GRADED)
                .content("Submission 2")
                .isLate(false)
                .submittedAt(LocalDateTime.now())
                .marks(BigDecimal.valueOf(92.50))
                .build();

        when(submissionService.listMySubmissions(eq(AUTH_HEADER)))
                .thenReturn(ApiResponse.<List<SubmissionResponse>>builder()
                        .status(HttpStatus.OK)
                        .message("Submissions fetched successfully")
                        .data(List.of(first, second))
                        .build());

        mockMvc.perform(get("/api/v1/submissions/my")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Submissions fetched successfully"))
                .andExpect(jsonPath("$.data[0].attemptNumber").value(1))
                .andExpect(jsonPath("$.data[1].attemptNumber").value(2));

        verify(submissionService).listMySubmissions(eq(AUTH_HEADER));
    }

    @Test
    void submitAssignment_whenValid_returnsSubmittedSubmission() throws Exception {
        UUID submissionId = UUID.randomUUID();

        SubmissionResponse response = SubmissionResponse.builder()
                .id(submissionId)
                .assignmentId(UUID.randomUUID())
                .studentId(UUID.randomUUID())
                .attemptNumber(1)
                .status(SubmissionStatus.SUBMITTED)
                .submittedAt(LocalDateTime.now())
                .build();

        when(submissionService.submitAssignment(eq(submissionId), eq(AUTH_HEADER)))
                .thenReturn(ApiResponse.<SubmissionResponse>builder()
                        .status(HttpStatus.OK)
                        .message("Assignment submitted successfully")
                        .data(response)
                        .build());

        mockMvc.perform(post("/api/v1/submissions/{submissionId}/submit", submissionId)
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Assignment submitted successfully"))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));

        verify(submissionService).submitAssignment(eq(submissionId), eq(AUTH_HEADER));
    }
}
