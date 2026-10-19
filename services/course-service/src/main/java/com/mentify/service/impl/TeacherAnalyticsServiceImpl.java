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
import com.mentify.client.dto.QuizGradebookResultResponse;
import com.mentify.client.dto.SubmissionDashboardResponse;
import com.mentify.dto.TeacherAnalyticsCourseTrendResponse;
import com.mentify.dto.TeacherAnalyticsMetricResponse;
import com.mentify.dto.TeacherAnalyticsResponse;
import com.mentify.entity.Course;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.CourseRepository;
import com.mentify.service.TeacherAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherAnalyticsServiceImpl implements TeacherAnalyticsService {

    private final CourseRepository courseRepository;
    private final TeacherCourseAccessGuard teacherCourseAccessGuard;
    private final AttendanceAnalyticsClient attendanceClient;
    private final AssignmentDashboardClient assignmentClient;
    private final QuizGradebookClient quizClient;
    private final ExamDashboardClient examClient;

    @Override
    public ApiResponse<TeacherAnalyticsResponse> getAnalytics(
            UUID courseId, LocalDate from, LocalDate to, String authorizationHeader) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }

        List<Course> courses = courses(courseId);
        if (teacherCourseAccessGuard.isTeacherAuthenticated()) {
            courses.forEach(teacherCourseAccessGuard::assertTeacherOwnsCourse);
        }

        Aggregate overall = new Aggregate();
        List<TeacherAnalyticsCourseTrendResponse> trends = new ArrayList<>();
        for (Course course : courses) {
            Aggregate current = new Aggregate();
            collectAttendance(course.getId(), from, to, authorizationHeader, current);
            collectAssignments(course.getId(), from, to, authorizationHeader, current);
            collectQuizzes(course.getId(), from, to, authorizationHeader, current);
            collectExams(course.getId(), from, to, authorizationHeader, current);
            overall.merge(current);
            trends.add(TeacherAnalyticsCourseTrendResponse.builder()
                    .courseId(course.getId()).courseName(course.getCourseName())
                    .attendanceRate(current.attendance.rate())
                    .assignmentAverage(current.assignments.average())
                    .quizAverage(current.quizzes.average())
                    .examAverage(current.exams.average()).build());
        }

        TeacherAnalyticsResponse data = TeacherAnalyticsResponse.builder()
                .attendance(overall.attendance.metric())
                .assignmentPerformance(overall.assignments.metric())
                .quizPerformance(overall.quizzes.metric())
                .examPerformance(overall.exams.metric())
                .courseTrends(trends).build();
        return ApiResponse.<TeacherAnalyticsResponse>builder().statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK).message("Teacher analytics fetched successfully").data(data).build();
    }

    private List<Course> courses(UUID courseId) {
        if (courseId != null) {
            return List.of(courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId)));
        }
        return teacherCourseAccessGuard.isTeacherAuthenticated()
                ? courseRepository.findAllByAssignedTeacherIdOrderByCreatedAtDesc(
                        teacherCourseAccessGuard.getAuthenticatedTeacherId())
                : courseRepository.findAll();
    }

    private void collectAttendance(UUID courseId, LocalDate from, LocalDate to, String auth, Aggregate aggregate) {
        ApiResponse<List<AttendanceSessionAnalyticsResponse>> response = attendanceClient.getSessions(courseId, auth);
        List<AttendanceSessionAnalyticsResponse> sessions = response == null || response.getData() == null
                ? List.of() : response.getData();
        for (AttendanceSessionAnalyticsResponse session : sessions) {
            if (!inRange(session.getAttendanceDate(), from, to) || session.getRecords() == null) continue;
            for (AttendanceRecordAnalyticsResponse record : session.getRecords()) {
                if (record.getStatus() == null || "NOT_MARKED".equals(record.getStatus())) continue;
                aggregate.attendance.total++;
                if ("PRESENT".equals(record.getStatus()) || "LATE".equals(record.getStatus())
                        || "EXCUSED".equals(record.getStatus())) aggregate.attendance.attended++;
            }
        }
    }

    private void collectAssignments(UUID courseId, LocalDate from, LocalDate to, String auth, Aggregate aggregate) {
        List<AssignmentDashboardResponse> assignments = data(assignmentClient.getAssignments(courseId, auth));
        for (AssignmentDashboardResponse assignment : assignments) {
            if (assignment.getId() == null || assignment.getMaxMarks() == null || assignment.getMaxMarks() <= 0) continue;
            for (SubmissionDashboardResponse submission : data(assignmentClient.getSubmissions(assignment.getId(), auth))) {
                if (!inRange(submission.getSubmittedAt() == null ? null : submission.getSubmittedAt().toLocalDate(), from, to)) continue;
                aggregate.assignments.total++;
                if (submission.getMarks() != null) {
                    aggregate.assignments.graded++;
                    aggregate.assignments.percentages.add(percent(submission.getMarks(), BigDecimal.valueOf(assignment.getMaxMarks())));
                }
            }
        }
    }

    private void collectQuizzes(UUID courseId, LocalDate from, LocalDate to, String auth, Aggregate aggregate) {
        for (QuizGradebookResponse quiz : data(quizClient.getQuizzes(courseId, auth))) {
            if (!inRange(quiz.getStartTime() == null ? null : quiz.getStartTime().toLocalDate(), from, to)) continue;
            if (quiz.getResults() == null) continue;
            for (QuizGradebookResultResponse result : quiz.getResults()) {
                if (result.getScore() == null) continue;
                aggregate.quizzes.total++;
                aggregate.quizzes.graded++;
                aggregate.quizzes.participants++;
                aggregate.quizzes.percentages.add(result.getPercentage() != null ? result.getPercentage()
                        : percent(result.getScore(), quiz.getTotalMarks()));
            }
        }
    }

    private void collectExams(UUID courseId, LocalDate from, LocalDate to, String auth, Aggregate aggregate) {
        for (ExamDashboardResponse exam : data(examClient.getExams(courseId, auth))) {
            if (!inRange(exam.getExamDate(), from, to)) continue;
            ApiResponse<ExamResultSheetResponse> response = examClient.getExamResults(exam.getId(), auth);
            ExamResultSheetResponse sheet = response == null ? null : response.getData();
            if (sheet == null || sheet.getResults() == null) continue;
            BigDecimal max = exam.getTotalMarks() != null ? exam.getTotalMarks() : sheet.getTotalMarks();
            for (ExamResultResponse result : sheet.getResults()) {
                if (result.getMarksObtained() == null || max == null || max.signum() <= 0) continue;
                aggregate.exams.total++;
                aggregate.exams.graded++;
                aggregate.exams.participants++;
                aggregate.exams.percentages.add(percent(result.getMarksObtained(), max));
                if (exam.getPassMarks() != null && result.getMarksObtained().compareTo(exam.getPassMarks()) >= 0) {
                    aggregate.exams.passed++;
                }
            }
        }
    }

    private <T> List<T> data(ApiResponse<List<T>> response) {
        return response == null || response.getData() == null ? List.of() : response.getData();
    }

    private boolean inRange(LocalDate date, LocalDate from, LocalDate to) {
        return date != null && (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    private BigDecimal percent(BigDecimal value, BigDecimal max) {
        if (value == null || max == null || max.signum() <= 0) return BigDecimal.ZERO;
        return value.multiply(BigDecimal.valueOf(100)).divide(max, 2, RoundingMode.HALF_UP);
    }

    private static class Aggregate {
        private final AttendanceAggregate attendance = new AttendanceAggregate();
        private final PerformanceAggregate assignments = new PerformanceAggregate();
        private final PerformanceAggregate quizzes = new PerformanceAggregate();
        private final PerformanceAggregate exams = new PerformanceAggregate();

        private void merge(Aggregate other) {
            attendance.total += other.attendance.total; attendance.attended += other.attendance.attended;
            assignments.merge(other.assignments); quizzes.merge(other.quizzes); exams.merge(other.exams);
        }
    }

    private static class AttendanceAggregate {
        private long total;
        private long attended;
        private BigDecimal rate() { return total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(attended * 100.0 / total).setScale(2, RoundingMode.HALF_UP); }
        private TeacherAnalyticsMetricResponse metric() { return TeacherAnalyticsMetricResponse.builder().total(total).attendanceRate(rate()).build(); }
    }

    private static class PerformanceAggregate {
        private long total;
        private long graded;
        private long participants;
        private long passed;
        private final List<BigDecimal> percentages = new ArrayList<>();
        private void merge(PerformanceAggregate other) { total += other.total; graded += other.graded; participants += other.participants; passed += other.passed; percentages.addAll(other.percentages); }
        private BigDecimal average() { return percentages.isEmpty() ? BigDecimal.ZERO : percentages.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(percentages.size()), 2, RoundingMode.HALF_UP); }
        private TeacherAnalyticsMetricResponse metric() { return TeacherAnalyticsMetricResponse.builder().total(total).graded(graded).participants(participants).passed(passed).averagePercentage(average()).build(); }
    }
}
