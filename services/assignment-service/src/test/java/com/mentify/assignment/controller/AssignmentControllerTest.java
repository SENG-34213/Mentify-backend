package com.mentify.assignment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.assignment.dto.request.CreateAssignmentRequest;
import com.mentify.assignment.dto.response.AssignmentResponse;
import com.mentify.assignment.dto.response.StudentAssignmentResponse;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AssignmentController.class,
        properties = "spring.cloud.config.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
class AssignmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AssignmentService assignmentService;

    @Test
    void createAssignment_whenValidRequest_returnsCreated() throws Exception {
        UUID courseId = UUID.randomUUID();
        AssignmentResponse assignmentResponse = AssignmentResponse.builder()
                .id(UUID.randomUUID())
                .courseId(courseId)
                .teacherId(UUID.randomUUID())
                .title("Java Homework")
                .dueDate(LocalDateTime.now().plusDays(7))
                .maxMarks(new BigDecimal("25.00"))
                .allowedAttempts(2)
                .status(AssignmentStatus.DRAFT)
                .build();

        when(assignmentService.createAssignment(any(CreateAssignmentRequest.class), eq("******"))).thenReturn(
                ApiResponse.<AssignmentResponse>builder()
                        .status(HttpStatus.CREATED)
                        .message("Assignment created successfully")
                        .data(assignmentResponse)
                        .build()
        );

        CreateAssignmentRequest request = CreateAssignmentRequest.builder()
                .courseId(courseId)
                .title("Java Homework")
                .dueDate(LocalDateTime.now().plusDays(7))
                .maxMarks(new BigDecimal("25.00"))
                .allowedAttempts(2)
                .build();

        mockMvc.perform(post("/api/assignments")
                        .header("Authorization", "******")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Assignment created successfully"))
                .andExpect(jsonPath("$.data.title").value("Java Homework"));

        verify(assignmentService).createAssignment(any(CreateAssignmentRequest.class), eq("******"));
    }

    @Test
    void createAssignment_whenTitleIsBlank_returnsBadRequest() throws Exception {
        CreateAssignmentRequest request = CreateAssignmentRequest.builder()
                .courseId(UUID.randomUUID())
                .title(" ")
                .dueDate(LocalDateTime.now().plusDays(2))
                .maxMarks(new BigDecimal("25.00"))
                .allowedAttempts(1)
                .build();

        mockMvc.perform(post("/api/assignments")
                        .header("Authorization", "******")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Assignment title is required"));

        verifyNoInteractions(assignmentService);
    }

    @Test
    void publishAssignment_returnsOk() throws Exception {
        UUID assignmentId = UUID.randomUUID();
        when(assignmentService.publishAssignment(assignmentId, "******")).thenReturn(
                ApiResponse.<AssignmentResponse>builder()
                        .status(HttpStatus.OK)
                        .message("Assignment published successfully")
                        .data(AssignmentResponse.builder()
                                .id(assignmentId)
                                .status(AssignmentStatus.PUBLISHED)
                                .title("Java Homework")
                                .build())
                        .build()
        );

        mockMvc.perform(post("/api/assignments/{assignmentId}/publish", assignmentId)
                        .header("Authorization", "******"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    void getPublishedAssignmentsByCourse_returnsOk() throws Exception {
        UUID courseId = UUID.randomUUID();
        when(assignmentService.getPublishedAssignmentsByCourse(courseId, "******")).thenReturn(
                ApiResponse.<List<StudentAssignmentResponse>>builder()
                        .status(HttpStatus.OK)
                        .message("Assignments fetched successfully")
                        .data(List.of(StudentAssignmentResponse.builder()
                                .id(UUID.randomUUID())
                                .courseId(courseId)
                                .title("Java Homework")
                                .build()))
                        .build()
        );

        mockMvc.perform(get("/api/courses/{courseId}/assignments", courseId)
                        .header("Authorization", "******"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Java Homework"));
    }
}
