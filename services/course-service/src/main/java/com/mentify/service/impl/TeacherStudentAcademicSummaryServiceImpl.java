package com.mentify.service.impl;

import com.mentify.client.AssignmentDashboardClient;
import com.mentify.client.AttendanceAnalyticsClient;
import com.mentify.client.EntrollmentServiceClient;
import com.mentify.client.ExamDashboardClient;
import com.mentify.client.QuizGradebookClient;
import com.mentify.client.dto.AssignmentDashboardResponse;
import com.mentify.client.dto.AttendanceRecordAnalyticsResponse;
import com.mentify.client.dto.AttendanceSessionAnalyticsResponse;
import com.mentify.client.dto.ExamDashboardResponse;
import com.mentify.client.dto.ExamResultResponse;
import com.mentify.client.dto.ExamResultSheetResponse;
import com.mentify.client.dto.QuizGradebookResponse;
import com.mentify.client.dto.SubmissionDashboardResponse;
import com.mentify.dto.TeacherStudentAcademicSummaryResponse;
import com.mentify.entity.Course;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.CourseRepository;
import com.mentify.service.TeacherStudentAcademicSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherStudentAcademicSummaryServiceImpl implements TeacherStudentAcademicSummaryService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final CourseRepository courseRepository;
    private final TeacherCourseAccessGuard accessGuard;
    private final EntrollmentServiceClient enrollmentClient;
    private final AttendanceAnalyticsClient attendanceClient;
    private final AssignmentDashboardClient assignmentClient;
    private final QuizGradebookClient quizClient;
    private final ExamDashboardClient examClient;

    @Override
    public ApiResponse<TeacherStudentAcademicSummaryResponse> getSummary(
            UUID studentId, UUID courseId, String authorizationHeader) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        if (accessGuard.isTeacherAuthenticated()) accessGuard.assertTeacherOwnsCourse(course);

        List<UUID> enrolled = data(enrollmentClient.getEnrolledStudentIdsByCourse(courseId, authorizationHeader));
        if (!enrolled.contains(studentId)) {
            throw new ResourceNotFoundException("Student", "id", studentId);
        }

        AttendanceTotals attendance = attendance(courseId, studentId, authorizationHeader);
        Category assignments = assignments(courseId, studentId, authorizationHeader);
        Category quizzes = quizzes(courseId, studentId, authorizationHeader);
        Category exams = exams(courseId, studentId, authorizationHeader);
        long pending = assignments.pending + quizzes.pending + exams.pending;
        BigDecimal totalMax = assignments.maximum.add(quizzes.maximum).add(exams.maximum);
        BigDecimal totalEarned = assignments.earned.add(quizzes.earned).add(exams.earned);
        BigDecimal overall = percentage(totalEarned, totalMax);

        TeacherStudentAcademicSummaryResponse summary = TeacherStudentAcademicSummaryResponse.builder()
                .studentId(studentId).courseId(courseId)
                .attendancePercentage(percentage(BigDecimal.valueOf(attendance.attended),
                        BigDecimal.valueOf(attendance.total)))
                .absentCount(attendance.absent)
                .assignmentAverage(assignments.average())
                .quizAverage(quizzes.average())
                .examAverage(exams.average())
                .overallPercentage(overall)
                .gpa(gpa(overall))
                .submissionsPending(pending)
                .lastActiveAt(java.util.stream.Stream.of(assignments.lastActive, quizzes.lastActive, exams.lastActive)
                        .filter(java.util.Objects::nonNull).max(LocalDateTime::compareTo).orElse(null))
                .build();
        return ApiResponse.<TeacherStudentAcademicSummaryResponse>builder()
                .statusCode(HttpStatus.OK.value()).status(HttpStatus.OK)
                .message("Student academic summary fetched successfully").data(summary).build();
    }

    private AttendanceTotals attendance(UUID courseId, UUID studentId, String auth) {
        AttendanceTotals totals = new AttendanceTotals();
        for (AttendanceSessionAnalyticsResponse session : data(attendanceClient.getSessions(courseId, auth))) {
            if (session.getRecords() == null) continue;
            for (AttendanceRecordAnalyticsResponse record : session.getRecords()) {
                if (!studentId.equals(record.getStudentId())) continue;
                if ("PRESENT".equals(record.getStatus()) || "LATE".equals(record.getStatus())) {
                    totals.attended++;
                    totals.total++;
                } else if ("ABSENT".equals(record.getStatus())) {
                    totals.absent++;
                    totals.total++;
                }
            }
        }
        return totals;
    }

    private Category assignments(UUID courseId, UUID studentId, String auth) {
        Category category = new Category();
        for (AssignmentDashboardResponse assessment : data(assignmentClient.getAssignments(courseId, auth))) {
            if (!"CLOSED".equals(assessment.getStatus()) || assessment.getMaxMarks() == null
                    || assessment.getMaxMarks() <= 0) continue;
            BigDecimal max = BigDecimal.valueOf(assessment.getMaxMarks());
            SubmissionDashboardResponse result = data(assignmentClient.getSubmissions(assessment.getId(), auth)).stream()
                    .filter(item -> studentId.equals(item.getStudentId()) && item.getMarks() != null)
                    .max(Comparator.comparing(SubmissionDashboardResponse::getAttemptNumber,
                            Comparator.nullsFirst(Integer::compareTo))
                            .thenComparing(SubmissionDashboardResponse::getSubmittedAt,
                                    Comparator.nullsFirst(LocalDateTime::compareTo))).orElse(null);
            category.add(max, result == null ? BigDecimal.ZERO : result.getMarks(), result != null);
            if (result != null && result.getSubmittedAt() != null) category.active(result.getSubmittedAt());
        }
        return category;
    }

    private Category quizzes(UUID courseId, UUID studentId, String auth) {
        Category category = new Category();
        for (QuizGradebookResponse assessment : data(quizClient.getQuizzes(courseId, auth))) {
            if (assessment.getTotalMarks() == null || assessment.getTotalMarks().signum() <= 0) continue;
            var result = data(assessment.getResults()).stream()
                    .filter(item -> studentId.equals(item.getStudentId()) && item.getScore() != null).findFirst().orElse(null);
            category.add(assessment.getTotalMarks(), result == null ? BigDecimal.ZERO : result.getScore(), result != null);
            if (result != null && result.getSubmittedAt() != null) category.active(result.getSubmittedAt());
        }
        return category;
    }

    private Category exams(UUID courseId, UUID studentId, String auth) {
        Category category = new Category();
        for (ExamDashboardResponse assessment : data(examClient.getExams(courseId, auth))) {
            if (!"COMPLETED".equals(assessment.getStatus())) continue;
            ApiResponse<ExamResultSheetResponse> response = examClient.getExamResults(assessment.getId(), auth);
            ExamResultSheetResponse sheet = response == null ? null : response.getData();
            BigDecimal max = assessment.getTotalMarks() != null ? assessment.getTotalMarks()
                    : sheet == null ? null : sheet.getTotalMarks();
            if (max == null || max.signum() <= 0) continue;
            ExamResultResponse result = sheet == null || sheet.getResults() == null ? null : sheet.getResults().stream()
                    .filter(item -> studentId.equals(item.getStudentId()) && item.getMarksObtained() != null).findFirst().orElse(null);
            category.add(max, result == null ? BigDecimal.ZERO : result.getMarksObtained(), result != null);
        }
        return category;
    }

    private BigDecimal percentage(BigDecimal earned, BigDecimal maximum) {
        return maximum.signum() == 0 ? BigDecimal.ZERO : earned.multiply(HUNDRED).divide(maximum, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal gpa(BigDecimal value) {
        if (value.compareTo(BigDecimal.valueOf(90)) >= 0) return BigDecimal.valueOf(4);
        if (value.compareTo(BigDecimal.valueOf(80)) >= 0) return BigDecimal.valueOf(3);
        if (value.compareTo(BigDecimal.valueOf(70)) >= 0) return BigDecimal.valueOf(2);
        if (value.compareTo(BigDecimal.valueOf(60)) >= 0) return BigDecimal.ONE;
        return BigDecimal.ZERO;
    }

    private <T> List<T> data(ApiResponse<List<T>> response) {
        return response == null || response.getData() == null ? List.of() : response.getData();
    }

    private <T> List<T> data(List<T> response) { return response == null ? List.of() : response; }

    private static class AttendanceTotals { private long total; private long attended; private long absent; }

    private static class Category {
        private BigDecimal earned = BigDecimal.ZERO;
        private BigDecimal maximum = BigDecimal.ZERO;
        private long pending;
        private long scored;
        private BigDecimal averageTotal = BigDecimal.ZERO;
        private LocalDateTime lastActive;

        private void add(BigDecimal max, BigDecimal score, boolean submitted) {
            maximum = maximum.add(max);
            earned = earned.add(score);
            if (submitted) { scored++; averageTotal = averageTotal.add(score.multiply(HUNDRED).divide(max, 2, RoundingMode.HALF_UP)); }
            else pending++;
        }
        private void active(LocalDateTime timestamp) { if (lastActive == null || timestamp.isAfter(lastActive)) lastActive = timestamp; }
        private BigDecimal average() { return scored == 0 ? BigDecimal.ZERO : averageTotal.divide(BigDecimal.valueOf(scored), 2, RoundingMode.HALF_UP); }
    }
}
