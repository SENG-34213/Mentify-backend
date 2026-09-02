package com.mentify.assignment.service.impl;

import com.mentify.assignment.dto.request.CreateAssignmentRequest;
import com.mentify.assignment.dto.request.UpdateAssignmentRequest;
import com.mentify.assignment.dto.response.AssignmentResponse;
import com.mentify.assignment.dto.response.StudentAssignmentResponse;
import com.mentify.assignment.entity.Assignment;
import com.mentify.assignment.enums.AssignmentStatus;
import com.mentify.assignment.repository.AssignmentRepository;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.client.CourseServiceClient;
import com.mentify.quiz.client.EnrollmentServiceClient;
import com.mentify.quiz.client.dto.CourseLookupResponse;
import com.mentify.quiz.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceImplTest {

    private static final String AUTH_HEADER = "******";

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private CourseServiceClient courseServiceClient;

    @Mock
    private EnrollmentServiceClient enrollmentServiceClient;

    @Mock
    private CurrentUserService currentUserService;

    private AssignmentServiceImpl assignmentService;

    @BeforeEach
    void setUp() {
        assignmentService = new AssignmentServiceImpl(
                assignmentRepository,
                courseServiceClient,
                enrollmentServiceClient,
                currentUserService
        );
    }

    @Test
    void teacherCanCreateAssignmentForAssignedCourse() {
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        CreateAssignmentRequest request = CreateAssignmentRequest.builder()
                .courseId(courseId)
                .title(" Java Homework ")
                .description(" Practice loops ")
                .instructions(" Submit a PDF ")
                .dueDate(LocalDateTime.now().plusDays(7))
                .maxMarks(new BigDecimal("25.00"))
                .allowedAttempts(2)
                .lateSubmissionAllowed(true)
                .latePenaltyPercentage(new BigDecimal("10.00"))
                .build();

        when(courseServiceClient.getCourseById(courseId, AUTH_HEADER)).thenReturn(courseResponse(courseId, teacherId));
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(teacherId);
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> {
            Assignment assignment = invocation.getArgument(0);
            assignment.setId(UUID.randomUUID());
            return assignment;
        });

        ApiResponse<AssignmentResponse> response = assignmentService.createAssignment(request, AUTH_HEADER);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getData().getStatus()).isEqualTo(AssignmentStatus.DRAFT);
        assertThat(response.getData().getTitle()).isEqualTo("Java Homework");
        assertThat(response.getData().getTeacherId()).isEqualTo(teacherId);

        ArgumentCaptor<Assignment> assignmentCaptor = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentRepository).save(assignmentCaptor.capture());
        assertThat(assignmentCaptor.getValue().getCourseId()).isEqualTo(courseId);
        assertThat(assignmentCaptor.getValue().getLatePenaltyPercentage()).isEqualByComparingTo("10.00");
    }

    @Test
    void publishAssignment_movesDraftToPublished() {
        UUID assignmentId = UUID.randomUUID();
        Assignment assignment = baseAssignment(assignmentId, AssignmentStatus.DRAFT);

        when(assignmentRepository.findByIdAndIsActiveTrue(assignmentId)).thenReturn(Optional.of(assignment));
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(assignment.getTeacherId());
        when(courseServiceClient.getCourseById(assignment.getCourseId(), AUTH_HEADER))
                .thenReturn(courseResponse(assignment.getCourseId(), assignment.getTeacherId()));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiResponse<AssignmentResponse> response = assignmentService.publishAssignment(assignmentId, AUTH_HEADER);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getData().getStatus()).isEqualTo(AssignmentStatus.PUBLISHED);
    }

    @Test
    void closeAssignment_requiresPublishedStatus() {
        UUID assignmentId = UUID.randomUUID();
        Assignment assignment = baseAssignment(assignmentId, AssignmentStatus.DRAFT);

        when(assignmentRepository.findByIdAndIsActiveTrue(assignmentId)).thenReturn(Optional.of(assignment));
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(assignment.getTeacherId());

        assertThatThrownBy(() -> assignmentService.closeAssignment(assignmentId, AUTH_HEADER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Assignment can only be closed from PUBLISHED status");
    }

    @Test
    void studentCanFetchPublishedAssignmentsByCourse() {
        UUID courseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Assignment assignment = baseAssignment(UUID.randomUUID(), AssignmentStatus.PUBLISHED);
        assignment.setCourseId(courseId);

        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(enrollmentServiceClient.isStudentEnrolledInCourse(studentId, courseId, AUTH_HEADER)).thenReturn(true);
        when(assignmentRepository.findByCourseIdAndStatusAndIsActiveTrueOrderByDueDateAsc(courseId, AssignmentStatus.PUBLISHED))
                .thenReturn(List.of(assignment));

        ApiResponse<List<StudentAssignmentResponse>> response =
                assignmentService.getPublishedAssignmentsByCourse(courseId, AUTH_HEADER);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getTitle()).isEqualTo(assignment.getTitle());
    }

    @Test
    void studentCannotFetchUnpublishedAssignment() {
        UUID assignmentId = UUID.randomUUID();
        Assignment assignment = baseAssignment(assignmentId, AssignmentStatus.DRAFT);

        when(assignmentRepository.findByIdAndIsActiveTrue(assignmentId)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> assignmentService.getStudentAssignment(assignmentId, AUTH_HEADER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Assignment not found");
    }

    @Test
    void updateAssignment_rejectsLessonWithoutModule() {
        UUID assignmentId = UUID.randomUUID();
        Assignment assignment = baseAssignment(assignmentId, AssignmentStatus.DRAFT);
        UpdateAssignmentRequest request = UpdateAssignmentRequest.builder()
                .courseId(assignment.getCourseId())
                .lessonId(UUID.randomUUID())
                .title("Updated assignment")
                .dueDate(LocalDateTime.now().plusDays(3))
                .maxMarks(new BigDecimal("20.00"))
                .allowedAttempts(1)
                .build();

        when(assignmentRepository.findByIdAndIsActiveTrue(assignmentId)).thenReturn(Optional.of(assignment));
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(assignment.getTeacherId());
        when(courseServiceClient.getCourseById(assignment.getCourseId(), AUTH_HEADER))
                .thenReturn(courseResponse(assignment.getCourseId(), assignment.getTeacherId()));

        assertThatThrownBy(() -> assignmentService.updateAssignment(assignmentId, request, AUTH_HEADER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Module ID is required when lesson ID is provided");
    }

    private Assignment baseAssignment(UUID assignmentId, AssignmentStatus status) {
        Assignment assignment = Assignment.builder()
                .courseId(UUID.randomUUID())
                .teacherId(UUID.randomUUID())
                .title("Assignment")
                .description("Description")
                .instructions("Instructions")
                .dueDate(LocalDateTime.now().plusDays(5))
                .maxMarks(new BigDecimal("15.00"))
                .allowedAttempts(1)
                .lateSubmissionAllowed(false)
                .latePenaltyPercentage(BigDecimal.ZERO)
                .status(status)
                .build();
        assignment.setId(assignmentId);
        return assignment;
    }

    private ApiResponse<CourseLookupResponse> courseResponse(UUID courseId, UUID teacherId) {
        CourseLookupResponse course = new CourseLookupResponse();
        course.setId(courseId);
        course.setAssignedTeacherId(teacherId);
        course.setPublished(true);
        course.setVisible(true);

        return ApiResponse.<CourseLookupResponse>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .data(course)
                .build();
    }
}
