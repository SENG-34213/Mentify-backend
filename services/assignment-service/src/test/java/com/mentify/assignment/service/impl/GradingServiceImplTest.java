package com.mentify.assignment.service.impl;

import com.mentify.assignment.dto.request.GradeSubmissionRequest;
import com.mentify.assignment.dto.response.SubmissionResponse;
import com.mentify.assignment.entity.Assignment;
import com.mentify.assignment.entity.Submission;
import com.mentify.assignment.enums.SubmissionStatus;
import com.mentify.assignment.repository.SubmissionRepository;
import com.mentify.assignment.service.SubmissionResponseMapper;
import com.mentify.common.notification.NotificationEventPublisher;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradingServiceImplTest {

    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private SubmissionResponseMapper submissionResponseMapper;
    @Mock
    private NotificationEventPublisher notificationEventPublisher;
    @Mock
    private CourseServiceClient courseServiceClient;

    private GradingServiceImpl gradingService;

    @BeforeEach
    void setUp() {
        gradingService = new GradingServiceImpl(
                submissionRepository,
                currentUserService,
                submissionResponseMapper
        );
    }

    @Test
    void teacherCanGradeOwnSubmissionAndOptionalNotificationCanBeAbsent() {
        UUID submissionId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        Submission submission = submission(submissionId, teacherId, UUID.randomUUID(), "Needs revision");
        allowTeacher(teacherId, false);
        when(submissionRepository.save(submission)).thenReturn(submission);
        when(submissionResponseMapper.toResponse(submission, "Bearer token")).thenReturn(mock(SubmissionResponse.class));

        ApiResponse<SubmissionResponse> response = gradingService.gradeSubmission(
                submissionId,
                new GradeSubmissionRequest(new BigDecimal("8.5"), "   "),
                "Bearer token"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.GRADED);
        assertThat(submission.getMarks()).isEqualByComparingTo("8.5");
        assertThat(submission.getFeedback()).isEqualTo("   ");
        assertThat(submission.getGradedBy()).isEqualTo(teacherId);
        verify(notificationEventPublisher, never()).publishForRecipient(any(), any(), any(), any(), any());
    }

    @Test
    void administratorCanGradeAndPublishesFeedbackNotification() {
        UUID submissionId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Submission submission = submission(submissionId, UUID.randomUUID(), studentId, "Well done");
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(adminId);
        when(submissionRepository.save(submission)).thenReturn(submission);
        when(submissionResponseMapper.toResponse(submission, null)).thenReturn(mock(SubmissionResponse.class));
        ReflectionTestUtils.setField(gradingService, "notificationEventPublisher", notificationEventPublisher);

        ApiResponse<SubmissionResponse> response = gradingService.gradeSubmission(
                submissionId,
                new GradeSubmissionRequest(new BigDecimal("10"), "Excellent"),
                null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(submission.getGradedBy()).isEqualTo(adminId);
        verify(notificationEventPublisher).publishForRecipient(
                eq(studentId.toString()), isNull(), eq("SUBMISSION_GRADED"), eq("Assignment graded"), anyString()
        );
    }

    @Test
    void gradingRejectsMissingOrNegativeMarksBeforeSaving() {
        UUID submissionId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        Submission submission = submission(submissionId, teacherId, UUID.randomUUID(), null);
        allowTeacher(teacherId, false);

        assertThatThrownBy(() -> gradingService.gradeSubmission(
                submissionId, new GradeSubmissionRequest(null, null), null
        )).isInstanceOf(RuntimeException.class).hasMessageContaining("required");
        assertThatThrownBy(() -> gradingService.gradeSubmission(
                submissionId, new GradeSubmissionRequest(new BigDecimal("-1"), null), null
        )).isInstanceOf(RuntimeException.class).hasMessageContaining("negative");
        verify(submissionRepository, never()).save(submission);
    }

    @Test
    void nonTeacherCannotGrade() {
        UUID submissionId = UUID.randomUUID();
        Submission submission = submission(submissionId, UUID.randomUUID(), UUID.randomUUID(), null);
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(false);

        assertThatThrownBy(() -> gradingService.gradeSubmission(
                submissionId, new GradeSubmissionRequest(BigDecimal.ONE, null), null
        )).isInstanceOf(RuntimeException.class).hasMessage("Access denied");
    }

    @Test
    void teacherCannotGradeAnotherTeachersAssignmentOrAnAssignmentWithoutOwner() {
        UUID submissionId = UUID.randomUUID();
        Submission otherTeacherSubmission = submission(submissionId, UUID.randomUUID(), UUID.randomUUID(), null);
        allowTeacher(UUID.randomUUID(), false);

        assertThatThrownBy(() -> gradingService.gradeSubmission(
                submissionId, new GradeSubmissionRequest(BigDecimal.ONE, null), null
        )).isInstanceOf(RuntimeException.class).hasMessageContaining("own assignments");

        Submission unownedSubmission = mock(Submission.class);
        when(unownedSubmission.getAssignment()).thenReturn(null);
        when(submissionRepository.findByIdAndIsActiveTrue(submissionId)).thenReturn(Optional.of(unownedSubmission));
        assertThatThrownBy(() -> gradingService.gradeSubmission(
                submissionId, new GradeSubmissionRequest(BigDecimal.ONE, null), null
        )).isInstanceOf(RuntimeException.class).hasMessageContaining("own assignments");
        assertThat(otherTeacherSubmission).isNotNull();
    }

    @Test
    void missingSubmissionIsRejected() {
        UUID submissionId = UUID.randomUUID();
        when(submissionRepository.findByIdAndIsActiveTrue(submissionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gradingService.returnSubmission(submissionId, null))
                .isInstanceOf(RuntimeException.class).hasMessage("Submission not found");
    }

    @Test
    void returnSubmissionHandlesBlankAndPresentFeedback() {
        UUID firstId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        Submission blankFeedback = submission(firstId, teacherId, UUID.randomUUID(), "  ");
        allowTeacher(teacherId, false);
        when(submissionRepository.save(blankFeedback)).thenReturn(blankFeedback);
        when(submissionResponseMapper.toResponse(blankFeedback, null)).thenReturn(mock(SubmissionResponse.class));

        ApiResponse<SubmissionResponse> firstResponse = gradingService.returnSubmission(firstId, null);

        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(blankFeedback.getStatus()).isEqualTo(SubmissionStatus.RETURNED);
        verify(notificationEventPublisher, never()).publishForRecipient(any(), any(), any(), any(), any());

        UUID secondId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Submission feedback = submission(secondId, teacherId, studentId, "Please add citations");
        when(submissionRepository.findByIdAndIsActiveTrue(secondId)).thenReturn(Optional.of(feedback));
        when(submissionRepository.save(feedback)).thenReturn(feedback);
        when(submissionResponseMapper.toResponse(feedback, null)).thenReturn(mock(SubmissionResponse.class));
        ReflectionTestUtils.setField(gradingService, "notificationEventPublisher", notificationEventPublisher);

        ApiResponse<SubmissionResponse> secondResponse = gradingService.returnSubmission(secondId, null);

        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        verify(notificationEventPublisher).publishForRecipient(
                eq(studentId.toString()), isNull(), eq("SUBMISSION_RETURNED"), eq("Assignment returned"), anyString()
        );

        UUID thirdId = UUID.randomUUID();
        Submission noFeedback = submission(thirdId, teacherId, UUID.randomUUID(), null);
        when(submissionRepository.findByIdAndIsActiveTrue(thirdId)).thenReturn(Optional.of(noFeedback));
        when(submissionRepository.save(noFeedback)).thenReturn(noFeedback);
        when(submissionResponseMapper.toResponse(noFeedback, null)).thenReturn(mock(SubmissionResponse.class));
        assertThat(gradingService.returnSubmission(thirdId, null).getStatusCode())
                .isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void courseLookupUsesNameAndOptionalCodeAndFallsBackOnNullOrFailure() {
        UUID submissionId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        Submission submission = submission(submissionId, teacherId, UUID.randomUUID(), null);
        allowTeacher(teacherId, false);
        when(submissionRepository.save(submission)).thenReturn(submission);
        when(submissionResponseMapper.toResponse(submission, "token")).thenReturn(mock(SubmissionResponse.class));
        ReflectionTestUtils.setField(gradingService, "courseServiceClient", courseServiceClient);

        ApiResponse<CourseLookupResponse> courseResponse = mock(ApiResponse.class);
        CourseLookupResponse course = mock(CourseLookupResponse.class);
        when(courseServiceClient.getCourseById(any(), eq("token"))).thenReturn(courseResponse);
        when(courseResponse.getData()).thenReturn(course);
        when(course.getCourseName()).thenReturn("Biology");
        when(course.getCourseCode()).thenReturn(null);
        gradingService.gradeSubmission(submissionId, new GradeSubmissionRequest(BigDecimal.ONE, null), "token");
        verify(courseServiceClient).getCourseById(any(), eq("token"));

        UUID secondId = UUID.randomUUID();
        Submission second = submission(secondId, teacherId, UUID.randomUUID(), null);
        when(submissionRepository.findByIdAndIsActiveTrue(secondId)).thenReturn(Optional.of(second));
        when(submissionRepository.save(second)).thenReturn(second);
        when(submissionResponseMapper.toResponse(second, "token")).thenReturn(mock(SubmissionResponse.class));
        when(courseServiceClient.getCourseById(any(), eq("token"))).thenThrow(new IllegalStateException("unavailable"));
        assertThat(gradingService.gradeSubmission(
                secondId, new GradeSubmissionRequest(BigDecimal.ONE, null), "token"
        ).getStatusCode()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void optionalNotificationIsSkippedWhenStudentIdIsMissing() {
        UUID submissionId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        Submission submission = submission(submissionId, teacherId, null, null);
        allowTeacher(teacherId, false);
        when(submissionRepository.save(submission)).thenReturn(submission);
        when(submissionResponseMapper.toResponse(submission, null)).thenReturn(mock(SubmissionResponse.class));
        ReflectionTestUtils.setField(gradingService, "notificationEventPublisher", notificationEventPublisher);

        gradingService.gradeSubmission(submissionId, new GradeSubmissionRequest(BigDecimal.ONE, null), null);

        verify(notificationEventPublisher, never()).publishForRecipient(any(), any(), any(), any(), any());
    }

    @Test
    void courseLookupFallsBackForMissingResponsesAndUsesNonblankDetailsWhenAvailable() {
        UUID teacherId = UUID.randomUUID();
        ApiResponse<CourseLookupResponse> responseWithoutData = mock(ApiResponse.class);
        when(responseWithoutData.getData()).thenReturn(null);
        CourseLookupResponse noName = mock(CourseLookupResponse.class);
        when(noName.getCourseName()).thenReturn(null);
        CourseLookupResponse blankName = mock(CourseLookupResponse.class);
        when(blankName.getCourseName()).thenReturn(" ");
        CourseLookupResponse blankCode = mock(CourseLookupResponse.class);
        when(blankCode.getCourseName()).thenReturn("Biology");
        when(blankCode.getCourseCode()).thenReturn(" ");
        CourseLookupResponse namedCourse = mock(CourseLookupResponse.class);
        when(namedCourse.getCourseName()).thenReturn("Biology");
        when(namedCourse.getCourseCode()).thenReturn("BIO-101");

        List<ApiResponse<CourseLookupResponse>> responses = Arrays.asList(
                null,
                responseWithoutData,
                courseResponse(noName),
                courseResponse(blankName),
                courseResponse(blankCode),
                courseResponse(namedCourse)
        );
        AtomicInteger responseIndex = new AtomicInteger();
        when(courseServiceClient.getCourseById(any(), anyString()))
                .thenAnswer(invocation -> responses.get(responseIndex.getAndIncrement()));
        ReflectionTestUtils.setField(gradingService, "courseServiceClient", courseServiceClient);

        for (int i = 0; i < responses.size(); i++) {
            assertThat(gradeWithCourseLookup(teacherId, "token")).isEqualTo(HttpStatus.OK.value());
        }
    }

    private void allowTeacher(UUID teacherId, boolean admin) {
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(admin);
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(teacherId);
    }

    private Submission submission(UUID id, UUID teacherId, UUID studentId, String feedback) {
        Assignment assignment = mock(Assignment.class);
        lenient().when(assignment.getCourseId()).thenReturn(UUID.randomUUID());
        lenient().when(assignment.getTeacherId()).thenReturn(teacherId);
        lenient().when(assignment.getTitle()).thenReturn("Research assignment");
        lenient().when(assignment.getMaxMarks()).thenReturn(10);
        lenient().when(assignment.getDueDate()).thenReturn(LocalDateTime.now());

        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setStudentId(studentId);
        submission.setFeedback(feedback);
        submission.setMarks(BigDecimal.ONE);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setAttemptNumber(1);
        submission.setStatus(SubmissionStatus.SUBMITTED);
        when(submissionRepository.findByIdAndIsActiveTrue(id)).thenReturn(Optional.of(submission));
        return submission;
    }

    private ApiResponse<CourseLookupResponse> courseResponse(CourseLookupResponse course) {
        ApiResponse<CourseLookupResponse> response = mock(ApiResponse.class);
        when(response.getData()).thenReturn(course);
        return response;
    }

    private int gradeWithCourseLookup(UUID teacherId, String token) {
        UUID submissionId = UUID.randomUUID();
        Submission submission = submission(submissionId, teacherId, UUID.randomUUID(), null);
        allowTeacher(teacherId, false);
        when(submissionRepository.save(submission)).thenReturn(submission);
        when(submissionResponseMapper.toResponse(submission, token)).thenReturn(mock(SubmissionResponse.class));
        return gradingService.gradeSubmission(
                submissionId, new GradeSubmissionRequest(BigDecimal.ONE, null), token
        ).getStatusCode();
    }
}
