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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
                .maxMarks(30)
                .allowedAttempts(3)
                .allowLateSubmission(true)
                .latePenaltyPercentage(10)
                .rubricItems(List.of(
                        rubricItem("Correctness", "40.25"),
                        rubricItem("Completeness", "59.75")))
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
        assertThat(response.getData().getMaxMarks()).isEqualTo(30);
        assertThat(response.getData().getStatus()).isEqualTo(AssignmentStatus.DRAFT);
        assertThat(response.getData().getRubricItems())
                .extracting("criterion", "percentageWeight")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Correctness", new BigDecimal("40.25")),
                        org.assertj.core.groups.Tuple.tuple("Completeness", new BigDecimal("59.75")));
    }

    @Test
    void validateRubricWeights_whenWeightsAreWithinRangeAndTotal100_accepts() {
        assignmentService.validateRubricWeights(List.of(
                rubricItem("No weight", "0"),
                rubricItem("Main criterion", "100")));
    }

    @Test
    void validateRubricWeights_whenWeightIsOutsideRange_throws() {
        assertThatThrownBy(() -> assignmentService.validateRubricWeights(List.of(
                rubricItem("Invalid criterion", "100.01"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rubric percentage weights must be between 0 and 100 with up to 2 decimal places");

        assertThatThrownBy(() -> assignmentService.validateRubricWeights(List.of(
                rubricItem("Invalid criterion", "-0.01"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rubric percentage weights must be between 0 and 100 with up to 2 decimal places");
    }

    @Test
    void validateRubricWeights_whenTotalIsNot100_throws() {
        assertThatThrownBy(() -> assignmentService.validateRubricWeights(List.of(
                rubricItem("First criterion", "40"),
                rubricItem("Second criterion", "59.99"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rubric percentage weights must total 100");
    }

    @Test
    void validateAssignmentDates_whenStartDateMissing_throws() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);

        assertThatThrownBy(() -> AssignmentServiceImpl.validateAssignmentDates(
                null, now.plusDays(1), now))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Start date and due date are required");
    }

    @Test
    void validateAssignmentDates_whenStartDateIsInPast_throws() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);

        assertThatThrownBy(() -> AssignmentServiceImpl.validateAssignmentDates(
                now.minusNanos(1), now.plusDays(1), now))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Assignment start date must be in the future");
    }

    @Test
    void validateAssignmentDates_whenStartDateEqualsCurrentTime_throws() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);

        assertThatThrownBy(() -> AssignmentServiceImpl.validateAssignmentDates(
                now, now.plusDays(1), now))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Assignment start date must be in the future");
    }

    @Test
    void validateAssignmentDates_whenStartDateIsInFuture_acceptsDates() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                AssignmentServiceImpl.validateAssignmentDates(
                        now.plusNanos(1_000), now.plusDays(1), now));
    }

    @Test
    void updateAssignment_whenStartDateIsInPast_throws() {
        UUID assignmentId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        Assignment assignment = draftAssignment(teacherId);
        assignment.setId(assignmentId);
        when(assignmentRepository.findByIdAndIsActiveTrue(assignmentId))
                .thenReturn(Optional.of(assignment));
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(teacherId);

        LocalDateTime now = LocalDateTime.now();
        UpdateAssignmentRequest request = UpdateAssignmentRequest.builder()
                .title("Essay")
                .startDate(now.minusDays(1))
                .dueDate(now.plusDays(1))
                .maxMarks(100)
                .allowedAttempts(1)
                .allowLateSubmission(false)
                .build();

        assertThatThrownBy(() -> assignmentService.updateAssignment(assignmentId, request, "test-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Assignment start date must be in the future");
    }

    @Test
    void patchAssignment_whenStartDateIsInPast_throws() {
        UUID assignmentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        Assignment assignment = draftAssignment(teacherId);
        assignment.setCourseId(courseId);
        assignment.setId(assignmentId);

        CourseLookupResponse course = new CourseLookupResponse();
        course.setId(courseId);
        when(assignmentRepository.findByIdAndIsActiveTrue(assignmentId))
                .thenReturn(Optional.of(assignment));
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(teacherId);
        when(courseServiceClient.getCourseById(courseId, "test-token"))
                .thenReturn(ApiResponse.<CourseLookupResponse>builder()
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .data(course)
                        .build());

        assertThatThrownBy(() -> assignmentService.patchAssignment(
                assignmentId,
                PatchAssignmentRequest.builder().startDate(LocalDateTime.now().minusDays(1)).build(),
                "test-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Assignment start date must be in the future");
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
                .percentageWeight(new BigDecimal("100.00"))
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

    private Assignment draftAssignment(UUID teacherId) {
        LocalDateTime now = LocalDateTime.now();
        return Assignment.builder()
                .courseId(UUID.randomUUID())
                .moduleId(UUID.randomUUID())
                .teacherId(teacherId)
                .title("Essay")
                .startDate(now.plusDays(1))
                .dueDate(now.plusDays(5))
                .maxMarks(100)
                .allowedAttempts(1)
                .allowLateSubmission(false)
                .status(AssignmentStatus.DRAFT)
                .build();
    }

    private static RubricItemRequest rubricItem(String criterion, String percentageWeight) {
        return RubricItemRequest.builder()
                .criterion(criterion)
                .percentageWeight(new BigDecimal(percentageWeight))
                .build();
    }
}
