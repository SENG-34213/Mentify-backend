package com.mentify.assignment.service.impl;

import com.mentify.assignment.entity.Assignment;
import com.mentify.assignment.enums.AssignmentStatus;
import com.mentify.assignment.repository.AssignmentRepository;
import com.mentify.assignment.repository.SubmissionRepository;
import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.client.EnrollmentServiceClient;
import com.mentify.quiz.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentAssignmentServiceImplTest {

    @Mock private AssignmentRepository assignmentRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private EnrollmentServiceClient enrollmentServiceClient;
    @Mock private CurrentUserService currentUserService;

    private StudentAssignmentServiceImpl service;
    private final UUID studentId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();
    private final String authorization = "Bearer token";

    @BeforeEach
    void setUp() {
        service = new StudentAssignmentServiceImpl(assignmentRepository, submissionRepository,
                enrollmentServiceClient, currentUserService);
        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
    }

    @Test
    void returnsPublishedAssignmentsForEnrolledCoursesWithPagination() {
        Assignment assignment = Assignment.builder().courseId(courseId).title("Essay")
                .startDate(LocalDateTime.now().minusDays(1)).dueDate(LocalDateTime.now().plusDays(2))
                .maxMarks(20).status(AssignmentStatus.PUBLISHED).build();
        assignment.setId(assignmentId);
        when(enrollmentServiceClient.getStudentCourseIds(studentId, authorization))
                .thenReturn(ApiResponse.success(200, "ok", List.of(courseId)));
        when(assignmentRepository.findByCourseIdInAndIsActiveTrueOrderByDueDateAsc(List.of(courseId)))
                .thenReturn(List.of(assignment));
        when(submissionRepository.findByAssignmentIdInAndStudentIdAndIsActiveTrueOrderByAttemptNumberDesc(
                List.of(assignmentId), studentId)).thenReturn(List.of());

        Page<com.mentify.assignment.dto.response.StudentAssignmentResponse> page = service.getAssignments(
                studentId, null, null, null, null, 0, 10, authorization);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(assignmentId);
        assertThat(page.getContent().get(0).getSubmissionStatus()).isNull();
    }

    @Test
    void preventsRequestingAssignmentsForAnotherStudent() {
        assertThatThrownBy(() -> service.getAssignments(UUID.randomUUID(), null, null,
                null, null, 0, 10, authorization)).isInstanceOf(AccessDeniedException.class);
        verify(enrollmentServiceClient, never()).getStudentCourseIds(studentId, authorization);
    }
}
