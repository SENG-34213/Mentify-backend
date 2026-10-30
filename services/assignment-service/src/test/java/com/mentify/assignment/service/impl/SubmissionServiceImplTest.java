package com.mentify.assignment.service.impl;

import com.mentify.assignment.dto.request.CreateSubmissionRequest;
import com.mentify.assignment.entity.Assignment;
import com.mentify.assignment.entity.Submission;
import com.mentify.assignment.enums.AssignmentStatus;
import com.mentify.assignment.exception.SubmissionConflictException;
import com.mentify.assignment.repository.AssignmentRepository;
import com.mentify.assignment.repository.SubmissionRepository;
import com.mentify.assignment.service.SubmissionService;
import com.mentify.assignment.service.SubmissionResponseMapper;
import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.client.EnrollmentServiceClient;
import com.mentify.quiz.client.UserServiceClient;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceImplTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private EnrollmentServiceClient enrollmentServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private SubmissionResponseMapper submissionResponseMapper;

    private SubmissionService submissionService;

    @BeforeEach
    void setUp() {
        submissionService = new SubmissionServiceImpl(
                assignmentRepository,
                submissionRepository,
                enrollmentServiceClient,
                userServiceClient,
                currentUserService,
                submissionResponseMapper
        );
    }

    @Test
    void createDraftSubmission_whenAssignmentPublishedAndEnrolled_createsDraft() {
        UUID assignmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        Assignment assignment = Assignment.builder()
                .courseId(courseId)
                .moduleId(UUID.randomUUID())
                .title("Essay")
                .startDate(LocalDateTime.now().minusDays(1))
                .dueDate(LocalDateTime.now().plusDays(3))
                .maxMarks(100)
                .allowedAttempts(3)
                .allowLateSubmission(true)
                .latePenaltyPercentage(10)
                .status(AssignmentStatus.PUBLISHED)
                .build();
        assignment.setId(assignmentId);

        when(assignmentRepository.findByIdAndIsActiveTrue(assignmentId)).thenReturn(Optional.of(assignment));
        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(userServiceClient.isStudentExists(studentId, "Bearer token")).thenReturn(true);
        when(enrollmentServiceClient.isStudentEnrolledInCourse(studentId, courseId, "Bearer token")).thenReturn(true);
        when(submissionRepository.countByAssignmentIdAndStudentIdAndIsActiveTrue(assignmentId, studentId)).thenReturn(0L);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> {
            Submission submission = invocation.getArgument(0);
            submission.setId(UUID.randomUUID());
            return submission;
        });
        when(submissionResponseMapper.toResponse(any(Submission.class), anyString()))
                .thenAnswer(invocation -> com.mentify.assignment.dto.response.SubmissionResponse.builder()
                        .attemptNumber(invocation.<Submission>getArgument(0).getAttemptNumber())
                        .build());

        ApiResponse<com.mentify.assignment.dto.response.SubmissionResponse> response =
                submissionService.createSubmission(assignmentId, new CreateSubmissionRequest(), "Bearer token");

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getData().getAttemptNumber()).isEqualTo(1);
    }

    @Test
    void submitAssignment_whenLateAndDisallowed_rejectsSubmission() {
        UUID assignmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        Assignment assignment = Assignment.builder()
                .courseId(courseId)
                .moduleId(UUID.randomUUID())
                .title("Timed assignment")
                .startDate(LocalDateTime.now().minusDays(10))
                .dueDate(LocalDateTime.now().minusDays(1))
                .maxMarks(100)
                .allowedAttempts(1)
                .allowLateSubmission(false)
                .latePenaltyPercentage(0)
                .status(AssignmentStatus.PUBLISHED)
                .build();
        assignment.setId(assignmentId);

        Submission submission = Submission.builder()
                .assignment(assignment)
                .studentId(studentId)
                .attemptNumber(1)
                .status(com.mentify.assignment.enums.SubmissionStatus.DRAFT)
                .build();
        submission.setId(UUID.randomUUID());

        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(submissionRepository.findByIdAndIsActiveTrue(submission.getId())).thenReturn(Optional.of(submission));

        org.junit.jupiter.api.Assertions.assertThrows(
                SubmissionConflictException.class,
                () -> submissionService.submitAssignment(submission.getId(), "Bearer token")
        );

        verify(submissionRepository, never()).save(any(Submission.class));
    }

    @Test
    void createSubmission_whenSubmissionExists_reopensExistingSubmission() {
        UUID assignmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        Assignment assignment = Assignment.builder()
                .courseId(courseId)
                .moduleId(UUID.randomUUID())
                .title("Limited assignment")
                .startDate(LocalDateTime.now().minusDays(1))
                .dueDate(LocalDateTime.now().plusDays(1))
                .maxMarks(100)
                .allowedAttempts(1)
                .allowLateSubmission(false)
                .status(AssignmentStatus.PUBLISHED)
                .build();
        assignment.setId(assignmentId);

        when(assignmentRepository.findByIdAndIsActiveTrue(assignmentId)).thenReturn(Optional.of(assignment));
        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(userServiceClient.isStudentExists(studentId, "******")).thenReturn(true);
        when(enrollmentServiceClient.isStudentEnrolledInCourse(studentId, courseId, "******")).thenReturn(true);
        Submission existing = Submission.builder().assignment(assignment).studentId(studentId).attemptNumber(1)
                .status(com.mentify.assignment.enums.SubmissionStatus.SUBMITTED).content("Existing answer")
                .build();
        when(submissionRepository.findFirstByAssignmentIdAndStudentIdAndIsActiveTrueOrderByAttemptNumberDesc(assignmentId, studentId)).thenReturn(Optional.of(existing));
        when(submissionRepository.save(existing)).thenReturn(existing);
        ApiResponse<?> response = submissionService.createSubmission(assignmentId, new CreateSubmissionRequest(), "******");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        verify(submissionRepository).save(existing);
    }
}
