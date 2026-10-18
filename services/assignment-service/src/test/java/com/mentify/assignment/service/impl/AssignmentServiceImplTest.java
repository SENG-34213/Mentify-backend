package com.mentify.assignment.service.impl;

import com.mentify.assignment.dto.request.CreateAssignmentRequest;
import com.mentify.assignment.dto.request.PatchAssignmentRequest;
import com.mentify.assignment.dto.request.RubricItemRequest;
import com.mentify.assignment.dto.response.AssignmentResponse;
import com.mentify.assignment.entity.Assignment;
import com.mentify.assignment.entity.AssignmentRubricItem;
import com.mentify.assignment.enums.AssignmentStatus;
import com.mentify.assignment.repository.AssignmentRepository;
import com.mentify.assignment.repository.SubmissionRepository;
import com.mentify.assignment.service.AssignmentService;
import com.mentify.assignment.service.CalendarProjectionService;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceImplTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private CourseServiceClient courseServiceClient;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private CalendarProjectionService calendarProjectionService;

    private AssignmentService assignmentService;

    @BeforeEach
    void setUp() {
        assignmentService = new AssignmentServiceImpl(
                assignmentRepository,
                submissionRepository,
                courseServiceClient,
                currentUserService,
                calendarProjectionService
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
                .rubricItems(List.of(RubricItemRequest.builder()
                        .criterion("Correctness")
                        .marks(75)
                        .build()))
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
        assertThat(response.getData().getRubricItems())
                .extracting("criterion", "marks")
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Correctness", 75));
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

    @Test
    void patchAssignment_whenOnlyTitleProvided_preservesOtherAssignmentValues() {
        UUID assignmentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        String authorization = "test-token";
        Assignment assignment = Assignment.builder()
                .courseId(courseId)
                .moduleId(UUID.randomUUID())
                .teacherId(teacherId)
                .title("Old title")
                .description("Existing description")
                .instructions("Existing instructions")
                .startDate(LocalDateTime.now().plusDays(1))
                .dueDate(LocalDateTime.now().plusDays(5))
                .maxMarks(100)
                .allowedAttempts(3)
                .allowLateSubmission(true)
                .latePenaltyPercentage(10)
                .status(AssignmentStatus.DRAFT)
                .build();
        assignment.setId(assignmentId);
        assignment.getRubricItems().add(AssignmentRubricItem.builder()
                .assignment(assignment)
                .criterion("Correctness")
                .marks(80)
                .displayOrder(0)
                .build());
        CourseLookupResponse course = new CourseLookupResponse();
        course.setId(courseId);

        when(assignmentRepository.findByIdAndIsActiveTrue(assignmentId)).thenReturn(Optional.of(assignment));
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(teacherId);
        when(courseServiceClient.getCourseById(courseId, authorization)).thenReturn(
                ApiResponse.<CourseLookupResponse>builder()
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .data(course)
                        .build()
        );
        when(assignmentRepository.save(any(Assignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApiResponse<AssignmentResponse> result = assignmentService.patchAssignment(
                assignmentId,
                PatchAssignmentRequest.builder().title("  New title  ").build(),
                authorization
        );

        assertThat(result.getData().getTitle()).isEqualTo("New title");
        assertThat(result.getData().getDescription()).isEqualTo("Existing description");
        assertThat(result.getData().getInstructions()).isEqualTo("Existing instructions");
        assertThat(result.getData().getMaxMarks()).isEqualTo(100);
        assertThat(result.getData().getAllowedAttempts()).isEqualTo(3);
        assertThat(result.getData().getAllowLateSubmission()).isTrue();
        assertThat(result.getData().getLatePenaltyPercentage()).isEqualTo(10);
        assertThat(result.getData().getRubricItems()).hasSize(1);
    }

    @Test
    void getAssignment_includesSubmissionCounts() {
        UUID assignmentId = UUID.randomUUID();
        Assignment assignment = Assignment.builder()
                .courseId(UUID.randomUUID())
                .moduleId(UUID.randomUUID())
                .teacherId(UUID.randomUUID())
                .title("Essay")
                .startDate(LocalDateTime.now().plusDays(1))
                .dueDate(LocalDateTime.now().plusDays(5))
                .maxMarks(100)
                .allowedAttempts(1)
                .status(AssignmentStatus.PUBLISHED)
                .build();
        assignment.setId(assignmentId);

        SubmissionRepository.AssignmentSubmissionCounts counts =
                mock(SubmissionRepository.AssignmentSubmissionCounts.class);
        when(counts.getTotalSubmissions()).thenReturn(12L);
        when(counts.getGradedSubmissions()).thenReturn(8L);
        when(assignmentRepository.findByIdAndIsActiveTrue(assignmentId)).thenReturn(Optional.of(assignment));
        when(submissionRepository.findSubmissionCountsByAssignmentIds(List.of(assignmentId)))
                .thenReturn(List.of(counts));

        AssignmentResponse response = assignmentService.getAssignment(assignmentId).getData();

        assertThat(response.getTotalSubmissions()).isEqualTo(12L);
        assertThat(response.getGradedCount()).isEqualTo(8L);
    }
}
