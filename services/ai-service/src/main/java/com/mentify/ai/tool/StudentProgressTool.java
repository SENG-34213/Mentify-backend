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
import com.mentify.payload.response.ApiResponse;
import com.mentify.ai.security.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class StudentProgressTool implements AiTool<StudentProgressSummary> {

    public static final String TOOL_NAME = "getStudentProgress";

    private final AuthenticatedUserService authenticatedUserService;
    private final StudentEnrollmentClient enrollmentClient;
    private final StudentProgressClient progressClient;
    private final StudentAttendanceClient attendanceClient;
    private final StudentAssignmentClient assignmentClient;
    private final StudentQuizClient quizClient;

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public StudentProgressSummary execute(String authorizationHeader) {
        UUID studentId = authenticatedUserService.getCurrentUserId();
        ApiResponse<List<UUID>> enrollmentResponse = enrollmentClient.getStudentCourseIds(studentId, authorizationHeader);
        List<UUID> courseIds = enrollmentResponse == null || enrollmentResponse.getData() == null
                ? List.of() : enrollmentResponse.getData();

        int completedLessons = 0;
        int totalLessons = 0;
        long attendanceSessions = 0;
        long attendedSessions = 0;
        int assignmentsTotal = 0;
        int assignmentsSubmitted = 0;
        int assignmentsMissing = 0;
        BigDecimal assignmentMarks = BigDecimal.ZERO;
        int assignmentScored = 0;
        int quizzesTotal = 0;
        int quizzesAttempted = 0;
        BigDecimal quizScores = BigDecimal.ZERO;
        int quizScored = 0;

        for (UUID courseId : courseIds) {
            StudentCourseProgress progress = data(progressClient.getCourseProgress(courseId, authorizationHeader));
            if (progress != null) {
                completedLessons += progress.completedLessons();
                totalLessons += progress.totalLessons();
            }

            StudentAttendanceSummary attendance = data(attendanceClient.getMyAttendance(courseId, authorizationHeader));
            if (attendance != null) {
                attendanceSessions += attendance.completedSessions();
                attendedSessions += attendance.attendedCount();
            }

            StudentAssignmentPage page = data(assignmentClient.getMyAssignments(courseId, 0, 100, authorizationHeader));
            List<StudentAssignmentSummary> assignments = page == null || page.content() == null ? List.of() : page.content();
            assignmentsTotal += assignments.size();
            for (StudentAssignmentSummary assignment : assignments) {
                if (assignment.submissionStatus() != null) assignmentsSubmitted++;
                if (assignment.submissionStatus() == null) assignmentsMissing++;
                if (assignment.marks() != null && assignment.maxMarks() != null && assignment.maxMarks() > 0) {
                    assignmentMarks = assignmentMarks.add(assignment.marks()
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(assignment.maxMarks()), 2, RoundingMode.HALF_UP));
                    assignmentScored++;
                }
            }

            List<StudentQuizSummary> quizzes = data(quizClient.getCourseQuizzes(courseId, authorizationHeader));
            if (quizzes == null) quizzes = List.of();
            quizzesTotal += quizzes.size();
            for (StudentQuizSummary quiz : quizzes) {
                if (quiz.attemptsUsed() != null && quiz.attemptsUsed() > 0) quizzesAttempted++;
                if (quiz.lastScorePercentage() != null) {
                    quizScores = quizScores.add(quiz.lastScorePercentage());
                    quizScored++;
                }
            }
        }

        return new StudentProgressSummary(
                studentId,
                courseIds.size(),
                completedLessons,
                totalLessons,
                percentage(completedLessons, totalLessons),
                percentage(attendedSessions, attendanceSessions),
                assignmentsTotal,
                assignmentsSubmitted,
                assignmentsMissing,
                average(assignmentMarks, assignmentScored),
                quizzesTotal,
                quizzesAttempted,
                average(quizScores, quizScored));
    }

    private <T> T data(ApiResponse<T> response) {
        return response == null ? null : response.getData();
    }

    private BigDecimal percentage(long numerator, long denominator) {
        return denominator == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(numerator * 100.0 / denominator).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal average(BigDecimal total, int count) {
        return count == 0 ? BigDecimal.ZERO : total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }
}
