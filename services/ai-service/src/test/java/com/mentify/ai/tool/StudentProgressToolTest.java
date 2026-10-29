package com.mentify.ai.tool;

import com.mentify.ai.client.StudentAssignmentClient;
import com.mentify.ai.client.StudentAttendanceClient;
import com.mentify.ai.client.StudentEnrollmentClient;
import com.mentify.ai.client.StudentProgressClient;
import com.mentify.ai.client.StudentQuizClient;
import com.mentify.ai.dto.tool.StudentAssignmentPage;
import com.mentify.ai.dto.tool.StudentAssignmentSummary;
import com.mentify.ai.dto.tool.StudentAttendanceSummary;
import com.mentify.ai.dto.tool.StudentCourseProgress;
import com.mentify.ai.dto.tool.StudentProgressSummary;
import com.mentify.ai.dto.tool.StudentQuizSummary;
import com.mentify.ai.security.AuthenticatedUserService;
import com.mentify.payload.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentProgressToolTest {

    private static final String AUTH = "Bearer student-token";

    @Mock private AuthenticatedUserService authenticatedUserService;
    @Mock private StudentEnrollmentClient enrollmentClient;
    @Mock private StudentProgressClient progressClient;
    @Mock private StudentAttendanceClient attendanceClient;
    @Mock private StudentAssignmentClient assignmentClient;
    @Mock private StudentQuizClient quizClient;

    @Test
    void aggregatesOnlyAuthenticatedStudentsEnrolledCourseData() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        when(authenticatedUserService.getCurrentUserId()).thenReturn(studentId);
        when(enrollmentClient.getStudentCourseIds(studentId, AUTH))
                .thenReturn(ApiResponse.success(200, "ok", List.of(courseId)));
        when(progressClient.getCourseProgress(courseId, AUTH)).thenReturn(ApiResponse.success(200, "ok",
                new StudentCourseProgress(courseId, 3, 5, 60, null, null)));
        when(attendanceClient.getMyAttendance(courseId, AUTH)).thenReturn(ApiResponse.success(200, "ok",
                new StudentAttendanceSummary(10, 6, 1, 3, 7, 70)));
        when(assignmentClient.getMyAssignments(eq(courseId), eq(0), eq(100), eq(AUTH)))
                .thenReturn(ApiResponse.success(200, "ok", new StudentAssignmentPage(List.of(
                        new StudentAssignmentSummary(UUID.randomUUID(), courseId, "A", "SUBMITTED", "SUBMITTED", 100, new BigDecimal("80")),
                        new StudentAssignmentSummary(UUID.randomUUID(), courseId, "B", "MISSING", null, 100, null)))));
        when(quizClient.getCourseQuizzes(courseId, AUTH)).thenReturn(ApiResponse.success(200, "ok", List.of(
                new StudentQuizSummary(UUID.randomUUID(), courseId, "Q", 1, new BigDecimal("75")))));

        StudentProgressSummary result = new StudentProgressTool(
                authenticatedUserService, enrollmentClient, progressClient,
                attendanceClient, assignmentClient, quizClient).execute(AUTH);

        assertThat(result.studentId()).isEqualTo(studentId);
        assertThat(result.courseCount()).isEqualTo(1);
        assertThat(result.lessonProgressPercentage()).isEqualByComparingTo("60.00");
        assertThat(result.attendancePercentage()).isEqualByComparingTo("70.00");
        assertThat(result.assignmentsTotal()).isEqualTo(2);
        assertThat(result.assignmentsSubmitted()).isEqualTo(1);
        assertThat(result.assignmentsMissing()).isEqualTo(1);
        assertThat(result.assignmentAverageScore()).isEqualByComparingTo("80.00");
        assertThat(result.quizzesTotal()).isEqualTo(1);
        assertThat(result.quizzesAttempted()).isEqualTo(1);
        assertThat(result.quizAverageScore()).isEqualByComparingTo("75.00");
    }
}
