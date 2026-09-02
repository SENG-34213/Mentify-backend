package com.mentify.assignment.service.impl;

import com.mentify.assignment.dto.request.CreateAssignmentRequest;
import com.mentify.assignment.dto.response.AssignmentResponse;
import com.mentify.assignment.entity.Assignment;
import com.mentify.assignment.enums.AssignmentStatus;
import com.mentify.assignment.repository.AssignmentRepository;
import com.mentify.assignment.service.AssignmentService;
import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.client.CourseServiceClient;
import com.mentify.quiz.client.dto.CourseLookupResponse;
import com.mentify.quiz.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceImplTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private CourseServiceClient courseServiceClient;

    @Mock
    private CurrentUserService currentUserService;

    private AssignmentService assignmentService;

    @BeforeEach
    void setUp() {
        assignmentService = new AssignmentServiceImpl(
                assignmentRepository,
                courseServiceClient,
                currentUserService
        );
    }

    @Test
    void createAssignment_whenTeacherOwnsCourse_returnsCreatedAssignment() {
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        CreateAssignmentRequest request = CreateAssignmentRequest.builder()
                .courseId(courseId)
                .moduleId(UUID.randomUUID())
                .lessonId(UUID.randomUUID())
                .title("Essay Assignment")
                .description("Write a short essay")
                .instructions("Follow the rubric")
                .startDate(LocalDateTime.now().plusDays(1))
                .dueDate(LocalDateTime.now().plusDays(5))
                .maxMarks(100)
                .allowedAttempts(3)
                .allowLateSubmission(true)
                .latePenaltyPercentage(10)
                .build();

        CourseLookupResponse course = new CourseLookupResponse();
        course.setId(courseId);
        course.setAssignedTeacherId(teacherId);

        when(courseServiceClient.getCourseById(courseId, "Bearer token")).thenReturn(ApiResponse.<CourseLookupResponse>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .data(course)
                .build());
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(teacherId);
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> {
            Assignment assignment = invocation.getArgument(0);
            assignment.setId(UUID.randomUUID());
            return assignment;
        });

        ApiResponse<AssignmentResponse> response = assignmentService.createAssignment(request, "Bearer token");

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getData().getTitle()).isEqualTo("Essay Assignment");
        assertThat(response.getData().getStatus()).isEqualTo(AssignmentStatus.DRAFT);
    }

    @Test
    void getAssignment_whenNotFound_throwsResourceNotFound() {
        UUID assignmentId = UUID.randomUUID();
        when(assignmentRepository.findByIdAndIsActiveTrue(assignmentId)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> assignmentService.getAssignment(assignmentId)
        );
    }
}
