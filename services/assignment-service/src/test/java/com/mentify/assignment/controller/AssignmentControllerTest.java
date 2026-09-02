package com.mentify.assignment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.assignment.dto.request.CreateAssignmentRequest;
import com.mentify.assignment.dto.request.UpdateAssignmentRequest;
import com.mentify.assignment.dto.response.AssignmentResponse;
import com.mentify.assignment.enums.AssignmentStatus;
import com.mentify.assignment.service.AssignmentService;
import com.mentify.payload.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AssignmentController.class,
        properties = "spring.cloud.config.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
class AssignmentControllerTest {

    private static final String AUTH_HEADER = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AssignmentService assignmentService;

    @Test
    void createAssignment_whenRequestIsValid_returnsCreatedAssignment() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        LocalDateTime startDate = LocalDateTime.now().plusDays(1);
        LocalDateTime dueDate = LocalDateTime.now().plusDays(5);

        CreateAssignmentRequest request = CreateAssignmentRequest.builder()
                .courseId(courseId)
                .moduleId(moduleId)
                .title("Essay Assignment")
                .description("Write a short essay")
                .instructions("Follow the rubric")
                .startDate(startDate)
                .dueDate(dueDate)
                .maxMarks(100)
                .allowedAttempts(2)
                .allowLateSubmission(true)
                .latePenaltyPercentage(10)
                .build();

        AssignmentResponse response = AssignmentResponse.builder()
                .id(UUID.randomUUID())
                .courseId(courseId)
                .moduleId(moduleId)
                .title("Essay Assignment")
                .description("Write a short essay")
                .instructions("Follow the rubric")
                .startDate(startDate)
                .dueDate(dueDate)
                .maxMarks(100)
                .allowedAttempts(2)
                .allowLateSubmission(true)
                .latePenaltyPercentage(10)
                .status(AssignmentStatus.DRAFT)
                .build();

        when(assignmentService.createAssignment(any(CreateAssignmentRequest.class), eq(AUTH_HEADER)))
                .thenReturn(ApiResponse.<AssignmentResponse>builder()
                        .status(HttpStatus.CREATED)
                        .message("Assignment created successfully")
                        .data(response)
                        .build());

        mockMvc.perform(post("/api/v1/assignments")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Assignment created successfully"))
                .andExpect(jsonPath("$.data.title").value("Essay Assignment"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        verify(assignmentService).createAssignment(any(CreateAssignmentRequest.class), eq(AUTH_HEADER));
    }

    @Test
    void updateAssignment_whenRequestIsValid_returnsUpdatedAssignment() throws Exception {
        UUID assignmentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        LocalDateTime startDate = LocalDateTime.now().plusDays(2);
        LocalDateTime dueDate = LocalDateTime.now().plusDays(6);

        UpdateAssignmentRequest request = UpdateAssignmentRequest.builder()
                .title("Updated Essay Assignment")
                .description("Updated description")
                .instructions("Updated rubric")
                .startDate(startDate)
                .dueDate(dueDate)
                .maxMarks(120)
                .allowedAttempts(3)
                .allowLateSubmission(false)
                .latePenaltyPercentage(0)
                .build();

        AssignmentResponse response = AssignmentResponse.builder()
                .id(assignmentId)
                .courseId(courseId)
                .moduleId(moduleId)
                .title("Updated Essay Assignment")
                .description("Updated description")
                .instructions("Updated rubric")
                .startDate(startDate)
                .dueDate(dueDate)
                .maxMarks(120)
                .allowedAttempts(3)
                .allowLateSubmission(false)
                .latePenaltyPercentage(0)
                .status(AssignmentStatus.DRAFT)
                .build();

        when(assignmentService.updateAssignment(eq(assignmentId), any(UpdateAssignmentRequest.class), eq(AUTH_HEADER)))
                .thenReturn(ApiResponse.<AssignmentResponse>builder()
                        .status(HttpStatus.OK)
                        .message("Assignment updated successfully")
                        .data(response)
                        .build());

        mockMvc.perform(put("/api/v1/assignments/{assignmentId}", assignmentId)
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Assignment updated successfully"))
                .andExpect(jsonPath("$.data.title").value("Updated Essay Assignment"));

        verify(assignmentService).updateAssignment(eq(assignmentId), any(UpdateAssignmentRequest.class), eq(AUTH_HEADER));
    }

    @Test
    void listAssignmentsForCourse_whenCourseExists_returnsAssignments() throws Exception {
        UUID courseId = UUID.randomUUID();

        AssignmentResponse first = AssignmentResponse.builder()
                .id(UUID.randomUUID())
                .courseId(courseId)
                .moduleId(UUID.randomUUID())
                .title("Assignment 1")
                .status(AssignmentStatus.PUBLISHED)
                .build();

        AssignmentResponse second = AssignmentResponse.builder()
                .id(UUID.randomUUID())
                .courseId(courseId)
                .moduleId(UUID.randomUUID())
                .title("Assignment 2")
                .status(AssignmentStatus.DRAFT)
                .build();

        when(assignmentService.listAssignmentsForCourse(eq(courseId), eq(AUTH_HEADER)))
                .thenReturn(ApiResponse.<List<AssignmentResponse>>builder()
                        .status(HttpStatus.OK)
                        .message("Assignments fetched successfully")
                        .data(List.of(first, second))
                        .build());

        mockMvc.perform(get("/api/v1/courses/{courseId}/assignments", courseId)
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Assignments fetched successfully"))
                .andExpect(jsonPath("$.data[0].title").value("Assignment 1"))
                .andExpect(jsonPath("$.data[1].title").value("Assignment 2"));

        verify(assignmentService).listAssignmentsForCourse(eq(courseId), eq(AUTH_HEADER));
    }

    @Test
    void publishAssignment_whenValid_returnsPublishedAssignment() throws Exception {
        UUID assignmentId = UUID.randomUUID();

        AssignmentResponse response = AssignmentResponse.builder()
                .id(assignmentId)
                .title("Published Assignment")
                .status(AssignmentStatus.PUBLISHED)
                .build();

        when(assignmentService.publishAssignment(eq(assignmentId), eq(AUTH_HEADER)))
                .thenReturn(ApiResponse.<AssignmentResponse>builder()
                        .status(HttpStatus.OK)
                        .message("Assignment published successfully")
                        .data(response)
                        .build());

        mockMvc.perform(post("/api/v1/assignments/{assignmentId}/publish", assignmentId)
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Assignment published successfully"))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        verify(assignmentService).publishAssignment(eq(assignmentId), eq(AUTH_HEADER));
    }
}
