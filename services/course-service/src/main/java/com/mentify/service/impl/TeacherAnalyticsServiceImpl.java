package com.mentify.service.impl;

import com.mentify.client.AssignmentDashboardClient;
import com.mentify.client.AttendanceAnalyticsClient;
import com.mentify.client.EntrollmentServiceClient;
import com.mentify.client.ExamDashboardClient;
import com.mentify.client.QuizGradebookClient;
import com.mentify.client.UserServiceClient;
import com.mentify.client.dto.AssignmentDashboardResponse;
import com.mentify.client.dto.AttendanceRecordAnalyticsResponse;
import com.mentify.client.dto.AttendanceSessionAnalyticsResponse;
import com.mentify.client.dto.ExamDashboardResponse;
import com.mentify.client.dto.ExamResultResponse;
import com.mentify.client.dto.ExamResultSheetResponse;
import com.mentify.client.dto.QuizGradebookResponse;
import com.mentify.client.dto.QuizGradebookResultResponse;
import com.mentify.client.dto.StudentDirectoryProfileResponse;
import com.mentify.client.dto.SubmissionDashboardResponse;
import com.mentify.dto.TeacherAnalyticsCourseTrendResponse;
import com.mentify.dto.TeacherAnalyticsMetricResponse;
import com.mentify.dto.TeacherAnalyticsResponse;
import com.mentify.dto.TeacherAnalyticsSummaryResponse;
import com.mentify.dto.TeacherAtRiskStudentResponse;
import com.mentify.dto.TeacherAttendanceCorrelationResponse;
import com.mentify.dto.TeacherGradeDistributionResponse;
import com.mentify.entity.Course;
import com.mentify.enums.TeacherAnalyticsPeriod;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherAnalyticsServiceImpl implements TeacherAnalyticsService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final List<String> GRADES = List.of("A", "B", "C", "D", "F");

    private final CourseRepository courseRepository;
    private final TeacherCourseAccessGuard teacherCourseAccessGuard;
    private final EntrollmentServiceClient enrollmentClient;
    private final UserServiceClient userServiceClient;
    private final AttendanceAnalyticsClient attendanceClient;
    private final AssignmentDashboardClient assignmentClient;
    private final QuizGradebookClient quizClient;
    private final ExamDashboardClient examClient;

    @Override
    public ApiResponse<TeacherAnalyticsResponse> getAnalytics(
            UUID courseId,
            TeacherAnalyticsPeriod period,
            LocalDate from,
            LocalDate to,
            String authorizationHeader) {
        if (period == null) {
            throw new IllegalArgumentException("period must be one of WEEK, MONTH, or TERM");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        if (teacherCourseAccessGuard.isTeacherAuthenticated()) {
            teacherCourseAccessGuard.assertTeacherOwnsCourse(course);
        }

        DateRange range = resolveRange(period, from, to);
        List<UUID> studentIds = data(enrollmentClient.getEnrolledStudentIdsByCourse(courseId, authorizationHeader))
                .stream().filter(id -> id != null).distinct().toList();
        Map<UUID, StudentAnalytics> students = new LinkedHashMap<>();
        studentIds.forEach(id -> students.put(id, new StudentAnalytics()));

        AttendanceTotals attendanceTotals = collectAttendance(courseId, range, authorizationHeader, students);
        CategoryTotals assignmentTotals = collectAssignments(courseId, range, authorizationHeader, students);
        CategoryTotals quizTotals = collectQuizzes(courseId, range, authorizationHeader, students);
        CategoryTotals examTotals = collectExams(courseId, range, authorizationHeader, students);

        Map<UUID, StudentDirectoryProfileResponse> profiles = profiles(studentIds, authorizationHeader);
        List<StudentAnalytics> assessedStudents = students.values().stream()
                .filter(student -> student.maximum.signum() > 0)
                .toList();
        BigDecimal averagePercentage = average(assessedStudents.stream()
                .map(StudentAnalytics::percentage).toList());
        BigDecimal averageGpa = average(assessedStudents.stream()
                .map(student -> gpa(student.percentage())).toList());
        BigDecimal attendanceAverage = average(students.values().stream()
                .filter(student -> student.attendanceTotal > 0)
                .map(StudentAnalytics::attendancePercentage).toList());

        List<TeacherGradeDistributionResponse> gradeDistribution =
                gradeDistribution(assessedStudents);
        List<TeacherAttendanceCorrelationResponse> correlation =
                attendanceCorrelation(assessedStudents);
        List<TeacherAtRiskStudentResponse> atRisk =
                atRiskStudents(students, profiles);

        long expectedSubmissions = students.size()
                * (assignmentTotals.assessments + quizTotals.assessments + examTotals.assessments);
        long submitted = assignmentTotals.submissions + quizTotals.submissions + examTotals.submissions;
        BigDecimal submissionRate = expectedSubmissions == 0 ? BigDecimal.ZERO
                : percentage(submitted, expectedSubmissions);

        TeacherAnalyticsResponse response = TeacherAnalyticsResponse.builder()
                .courseId(courseId)
                .period(period.name())
                .summary(TeacherAnalyticsSummaryResponse.builder()
                        .studentCount(students.size())
                        .submissionRate(submissionRate)
                        .attendanceAverage(attendanceAverage)
                        .averagePercentage(averagePercentage)
                        .gpa(averageGpa)
                        .interventionCount(atRisk.size())
                        .build())
                .gradeDistribution(gradeDistribution)
                .attendanceCorrelation(correlation)
                .atRiskStudents(atRisk)
                .attendance(TeacherAnalyticsMetricResponse.builder()
                        .total(attendanceTotals.total)
                        .attendanceRate(attendanceAverage)
                        .build())
                .assignmentPerformance(assignmentTotals.metric())
                .quizPerformance(quizTotals.metric())
                .examPerformance(examTotals.metric())
                .courseTrends(List.of(TeacherAnalyticsCourseTrendResponse.builder()
                        .courseId(course.getId())
                        .courseName(course.getCourseName())
                        .attendanceRate(attendanceAverage)
                        .assignmentAverage(assignmentTotals.average())
                        .quizAverage(quizTotals.average())
                        .examAverage(examTotals.average())
                        .build()))
                .build();
        return ApiResponse.<TeacherAnalyticsResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .message("Teacher analytics fetched successfully")
                .data(response)
                .build();
    }

    private AttendanceTotals collectAttendance(
            UUID courseId,
            DateRange range,
            String authorizationHeader,
            Map<UUID, StudentAnalytics> students) {
        List<AttendanceSessionAnalyticsResponse> sessions =
                data(attendanceClient.getSessions(courseId, authorizationHeader));
        AttendanceTotals totals = new AttendanceTotals();
        for (AttendanceSessionAnalyticsResponse session : sessions) {
            if (!range.contains(session.getAttendanceDate()) || session.getRecords() == null) {
                continue;
            }
            for (AttendanceRecordAnalyticsResponse record : session.getRecords()) {
                StudentAnalytics student = students.get(record.getStudentId());
                if (student == null || record.getStatus() == null) {
                    continue;
                }
                String status = record.getStatus();
                if ("EXCUSED".equals(status) || "NOT_MARKED".equals(status)) {
                    continue;
                }
                if (!Set.of("PRESENT", "LATE", "ABSENT").contains(status)) {
                    continue;
                }
                student.attendanceTotal++;
                totals.total++;
                if ("PRESENT".equals(status) || "LATE".equals(status)) {
                    student.attendanceAttended++;
                    totals.attended++;
                }
            }
        }
        return totals;
    }

    private CategoryTotals collectAssignments(
            UUID courseId,
            DateRange range,
            String authorizationHeader,
            Map<UUID, StudentAnalytics> students) {
        CategoryTotals totals = new CategoryTotals();
        for (AssignmentDashboardResponse assignment :
                data(assignmentClient.getAssignments(courseId, authorizationHeader))) {
            if (!"CLOSED".equals(assignment.getStatus())
                    || assignment.getId() == null
                    || assignment.getMaxMarks() == null
                    || assignment.getMaxMarks() <= 0
                    || assignment.getDueDate() == null
                    || !range.contains(assignment.getDueDate().toLocalDate())) {
                continue;
            }
            BigDecimal maximum = BigDecimal.valueOf(assignment.getMaxMarks());
            Map<UUID, SubmissionDashboardResponse> latestSubmissions =
                    data(assignmentClient.getSubmissions(assignment.getId(), authorizationHeader)).stream()
                            .filter(submission -> submission.getStudentId() != null)
                            .filter(submission -> submission.getSubmittedAt() != null
                                    && range.contains(submission.getSubmittedAt().toLocalDate()))
                            .sorted(Comparator.comparing(SubmissionDashboardResponse::getAttemptNumber,
                                            Comparator.nullsFirst(Integer::compareTo))
                                    .thenComparing(SubmissionDashboardResponse::getSubmittedAt,
                                            Comparator.nullsFirst(Comparator.naturalOrder())))
                            .collect(Collectors.toMap(
                                    SubmissionDashboardResponse::getStudentId,
                                    Function.identity(),
                                    (first, latest) -> latest
                            ));
            totals.assessments++;
            for (Map.Entry<UUID, StudentAnalytics> entry : students.entrySet()) {
                SubmissionDashboardResponse submission = latestSubmissions.get(entry.getKey());
                boolean submitted = submission != null;
                BigDecimal score = submitted && submission.getMarks() != null
                        ? submission.getMarks() : BigDecimal.ZERO;
                entry.getValue().addAssessment(score, maximum, submitted);
                totals.record(score, maximum, submitted);
            }
        }
        return totals;
    }

    private CategoryTotals collectQuizzes(
            UUID courseId,
            DateRange range,
            String authorizationHeader,
            Map<UUID, StudentAnalytics> students) {
        CategoryTotals totals = new CategoryTotals();
        for (QuizGradebookResponse quiz : data(quizClient.getQuizzes(courseId, authorizationHeader))) {
            if (quiz.getQuizId() == null
                    || quiz.getTotalMarks() == null
                    || quiz.getTotalMarks().signum() <= 0
                    || quiz.getStartTime() == null
                    || !range.contains(quiz.getStartTime().toLocalDate())) {
                continue;
            }
            Map<UUID, QuizGradebookResultResponse> results = quiz.getResults() == null ? Map.of()
                    : quiz.getResults().stream()
                    .filter(result -> result.getStudentId() != null && result.getScore() != null)
                    .collect(Collectors.toMap(QuizGradebookResultResponse::getStudentId,
                            Function.identity(), (first, latest) -> latest));
            totals.assessments++;
            for (Map.Entry<UUID, StudentAnalytics> entry : students.entrySet()) {
                QuizGradebookResultResponse result = results.get(entry.getKey());
                boolean submitted = result != null;
                BigDecimal score = submitted ? result.getScore() : BigDecimal.ZERO;
                entry.getValue().addAssessment(score, quiz.getTotalMarks(), submitted);
                totals.record(score, quiz.getTotalMarks(), submitted);
            }
        }
        return totals;
    }

    private CategoryTotals collectExams(
            UUID courseId,
            DateRange range,
            String authorizationHeader,
            Map<UUID, StudentAnalytics> students) {
        CategoryTotals totals = new CategoryTotals();
        for (ExamDashboardResponse exam : data(examClient.getExams(courseId, authorizationHeader))) {
            if (!"COMPLETED".equals(exam.getStatus())
                    || exam.getId() == null
                    || exam.getExamDate() == null
                    || !range.contains(exam.getExamDate())) {
                continue;
            }
            ApiResponse<ExamResultSheetResponse> response =
                    examClient.getExamResults(exam.getId(), authorizationHeader);
            ExamResultSheetResponse sheet = response == null ? null : response.getData();
            BigDecimal maximum = exam.getTotalMarks() != null ? exam.getTotalMarks()
                    : sheet == null ? null : sheet.getTotalMarks();
            if (maximum == null || maximum.signum() <= 0) {
                continue;
            }
            Map<UUID, ExamResultResponse> results = sheet == null || sheet.getResults() == null ? Map.of()
                    : sheet.getResults().stream()
                    .filter(result -> result.getStudentId() != null && result.getMarksObtained() != null)
                    .collect(Collectors.toMap(ExamResultResponse::getStudentId,
                            Function.identity(), (first, latest) -> latest));
            totals.assessments++;
            for (Map.Entry<UUID, StudentAnalytics> entry : students.entrySet()) {
                ExamResultResponse result = results.get(entry.getKey());
                boolean submitted = result != null;
                BigDecimal score = submitted ? result.getMarksObtained() : BigDecimal.ZERO;
                entry.getValue().addAssessment(score, maximum, submitted);
                totals.record(score, maximum, submitted);
            }
        }
        return totals;
    }

    private Map<UUID, StudentDirectoryProfileResponse> profiles(
            List<UUID> studentIds, String authorizationHeader) {
        if (studentIds.isEmpty()) {
            return Map.of();
        }
        ApiResponse<List<StudentDirectoryProfileResponse>> response =
                userServiceClient.getStudentsByIds(studentIds, authorizationHeader);
        if (response == null || response.getData() == null) {
            return Map.of();
        }
        return response.getData().stream()
                .filter(profile -> profile.getId() != null)
                .collect(Collectors.toMap(StudentDirectoryProfileResponse::getId,
                        Function.identity(), (first, ignored) -> first));
    }

    private List<TeacherGradeDistributionResponse> gradeDistribution(List<StudentAnalytics> students) {
        if (students.isEmpty()) {
            return List.of();
        }
        Map<String, Long> counts = students.stream()
                .collect(Collectors.groupingBy(student -> grade(student.percentage()), Collectors.counting()));
        return GRADES.stream()
                .filter(counts::containsKey)
                .map(grade -> TeacherGradeDistributionResponse.builder()
                        .grade(grade)
                        .count(counts.get(grade))
                        .percentage(percentage(counts.get(grade), students.size()))
                        .build())
                .toList();
    }

    private List<TeacherAttendanceCorrelationResponse> attendanceCorrelation(List<StudentAnalytics> students) {
        Map<String, List<StudentAnalytics>> groups = students.stream()
                .filter(student -> student.attendanceTotal > 0)
                .collect(Collectors.groupingBy(student -> attendanceRange(student.attendancePercentage())));
        return List.of("0-59", "60-69", "70-79", "80-89", "90-100").stream()
                .filter(groups::containsKey)
                .map(range -> {
                    List<StudentAnalytics> group = groups.get(range);
                    return TeacherAttendanceCorrelationResponse.builder()
                            .attendanceRange(range)
                            .averageAssessmentPercentage(average(group.stream()
                                    .map(StudentAnalytics::percentage).toList()))
                            .studentCount(group.size())
                            .build();
                })
                .toList();
    }

    private List<TeacherAtRiskStudentResponse> atRiskStudents(
            Map<UUID, StudentAnalytics> students,
            Map<UUID, StudentDirectoryProfileResponse> profiles) {
        return students.entrySet().stream()
                .filter(entry -> entry.getValue().isAtRisk())
                .map(entry -> {
                    StudentAnalytics metrics = entry.getValue();
                    boolean lowAttendance = metrics.attendanceTotal > 0
                            && metrics.attendancePercentage().compareTo(BigDecimal.valueOf(75)) < 0;
                    boolean lowPerformance = metrics.maximum.signum() > 0
                            && metrics.percentage().compareTo(BigDecimal.valueOf(60)) < 0;
                    StudentDirectoryProfileResponse profile = profiles.get(entry.getKey());
                    return TeacherAtRiskStudentResponse.builder()
                            .studentId(entry.getKey())
                            .studentName(profile == null || profile.getName() == null
                                    ? "Unknown Student" : profile.getName())
                            .attendancePercentage(metrics.attendanceTotal == 0
                                    ? null : metrics.attendancePercentage())
                            .averagePercentage(metrics.maximum.signum() == 0
                                    ? BigDecimal.ZERO : metrics.percentage())
                            .reason(lowAttendance && lowPerformance ? "LOW_ATTENDANCE_AND_PERFORMANCE"
                                    : lowAttendance ? "LOW_ATTENDANCE" : "LOW_PERFORMANCE")
                            .build();
                })
                .toList();
    }

    private DateRange resolveRange(TeacherAnalyticsPeriod period, LocalDate from, LocalDate to) {
        if (from != null || to != null) {
            return new DateRange(from, to);
        }
        LocalDate end = LocalDate.now();
        LocalDate start = switch (period) {
            case WEEK -> end.minusDays(6);
            case MONTH -> end.minusDays(29);
            case TERM -> end.minusDays(89);
        };
        return new DateRange(start, end);
    }

    private <T> List<T> data(ApiResponse<List<T>> response) {
        return response == null || response.getData() == null ? List.of() : response.getData();
    }

    private BigDecimal average(List<BigDecimal> values) {
        return values.isEmpty() ? BigDecimal.ZERO
                : values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(long value, long total) {
        return total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(value).multiply(HUNDRED)
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private String grade(BigDecimal percentage) {
        if (percentage.compareTo(BigDecimal.valueOf(90)) >= 0) return "A";
        if (percentage.compareTo(BigDecimal.valueOf(80)) >= 0) return "B";
        if (percentage.compareTo(BigDecimal.valueOf(70)) >= 0) return "C";
        if (percentage.compareTo(BigDecimal.valueOf(60)) >= 0) return "D";
        return "F";
    }

    private BigDecimal gpa(BigDecimal percentage) {
        return switch (grade(percentage)) {
            case "A" -> BigDecimal.valueOf(4);
            case "B" -> BigDecimal.valueOf(3);
            case "C" -> BigDecimal.valueOf(2);
            case "D" -> BigDecimal.ONE;
            default -> BigDecimal.ZERO;
        };
    }

    private String attendanceRange(BigDecimal percentage) {
        if (percentage.compareTo(BigDecimal.valueOf(60)) < 0) return "0-59";
        if (percentage.compareTo(BigDecimal.valueOf(70)) < 0) return "60-69";
        if (percentage.compareTo(BigDecimal.valueOf(80)) < 0) return "70-79";
        if (percentage.compareTo(BigDecimal.valueOf(90)) < 0) return "80-89";
        return "90-100";
    }

    private record DateRange(LocalDate from, LocalDate to) {
        private boolean contains(LocalDate date) {
            return date != null
                    && (from == null || !date.isBefore(from))
                    && (to == null || !date.isAfter(to));
        }
    }

    private static class AttendanceTotals {
        private long total;
        private long attended;
    }

    private static class CategoryTotals {
        private long assessments;
        private long submissions;
        private final List<BigDecimal> percentages = new ArrayList<>();

        private void record(BigDecimal score, BigDecimal maximum, boolean submitted) {
            if (submitted) {
                submissions++;
            }
            percentages.add(score.multiply(HUNDRED).divide(maximum, 2, RoundingMode.HALF_UP));
        }

        private BigDecimal average() {
            return percentages.isEmpty() ? BigDecimal.ZERO
                    : percentages.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(percentages.size()), 2, RoundingMode.HALF_UP);
        }

        private TeacherAnalyticsMetricResponse metric() {
            long total = assessments == 0 ? 0 : percentages.size();
            return TeacherAnalyticsMetricResponse.builder()
                    .total(total)
                    .graded(submissions)
                    .participants(submissions)
                    .averagePercentage(average())
                    .build();
        }
    }

    private static class StudentAnalytics {
        private BigDecimal earned = BigDecimal.ZERO;
        private BigDecimal maximum = BigDecimal.ZERO;
        private long attendanceTotal;
        private long attendanceAttended;

        private void addAssessment(BigDecimal score, BigDecimal max, boolean submitted) {
            maximum = maximum.add(max);
            earned = earned.add(score);
        }

        private BigDecimal percentage() {
            return maximum.signum() == 0 ? BigDecimal.ZERO
                    : earned.multiply(HUNDRED).divide(maximum, 2, RoundingMode.HALF_UP);
        }

        private BigDecimal attendancePercentage() {
            return attendanceTotal == 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(attendanceAttended).multiply(HUNDRED)
                    .divide(BigDecimal.valueOf(attendanceTotal), 2, RoundingMode.HALF_UP);
        }

        private boolean isAtRisk() {
            return (attendanceTotal > 0 && attendancePercentage().compareTo(BigDecimal.valueOf(75)) < 0)
                    || (maximum.signum() > 0 && percentage().compareTo(BigDecimal.valueOf(60)) < 0);
        }
    }
}
