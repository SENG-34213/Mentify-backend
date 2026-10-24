package com.mentify.service.impl;

import com.mentify.client.AssignmentDashboardClient;
import com.mentify.client.EntrollmentServiceClient;
import com.mentify.client.ExamDashboardClient;
import com.mentify.client.QuizGradebookClient;
import com.mentify.client.UserServiceClient;
import com.mentify.client.dto.AssignmentDashboardResponse;
import com.mentify.client.dto.ExamDashboardResponse;
import com.mentify.client.dto.ExamResultResponse;
import com.mentify.client.dto.ExamResultSheetResponse;
import com.mentify.client.dto.QuizGradebookResponse;
import com.mentify.client.dto.QuizGradebookResultResponse;
import com.mentify.client.dto.StudentDirectoryProfileResponse;
import com.mentify.client.dto.SubmissionDashboardResponse;
import com.mentify.dto.TeacherGradebookItemResponse;
import com.mentify.dto.TeacherGradebookResponse;
import com.mentify.dto.TeacherGradebookStudentResponse;
import com.mentify.dto.TeacherGradebookSummaryResponse;
import com.mentify.entity.Course;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.CourseRepository;
import com.mentify.service.TeacherGradebookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherGradebookServiceImpl implements TeacherGradebookService {

    private static final Map<String, Integer> GRADING_SCALE = Map.of("A", 90, "B", 80, "C", 70, "D", 60, "F", 0);

    private final CourseRepository courseRepository;
    private final TeacherCourseAccessGuard teacherCourseAccessGuard;
    private final EntrollmentServiceClient entrollmentServiceClient;
    private final UserServiceClient userServiceClient;
    private final AssignmentDashboardClient assignmentDashboardClient;
    private final QuizGradebookClient quizGradebookClient;
    private final ExamDashboardClient examDashboardClient;

    @Override
    public ApiResponse<TeacherGradebookResponse> getGradebook(UUID courseId, String authorizationHeader) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        if (teacherCourseAccessGuard.isTeacherAuthenticated()) {
            teacherCourseAccessGuard.assertTeacherOwnsCourse(course);
        }

        List<UUID> studentIds = enrolledStudentIds(courseId, authorizationHeader);
        Map<UUID, StudentDirectoryProfileResponse> profiles = studentProfiles(studentIds, authorizationHeader);
        List<AssignmentDashboardResponse> assignments = assignments(courseId, authorizationHeader);
        Map<UUID, List<SubmissionDashboardResponse>> submissions = new LinkedHashMap<>();
        assignments.forEach(assignment -> submissions.put(assignment.getId(),
                submissions(assignment.getId(), authorizationHeader)));
        List<QuizGradebookResponse> quizzes = quizzes(courseId, authorizationHeader);
        List<ExamDashboardResponse> exams = exams(courseId, authorizationHeader);
        Map<UUID, ExamResultSheetResponse> examResults = new LinkedHashMap<>();
        exams.forEach(exam -> examResults.put(exam.getId(), examResults(exam.getId(), authorizationHeader)));

        List<TeacherGradebookStudentResponse> students = studentIds.stream().distinct().map(studentId -> {
            List<TeacherGradebookItemResponse> assignmentItems = assignments.stream()
                    .map(assignment -> assignmentItem(assignment, submissions.getOrDefault(assignment.getId(), List.of()), studentId))
                    .filter(Objects::nonNull)
                    .toList();
            List<TeacherGradebookItemResponse> quizItems = quizzes.stream()
                    .map(quiz -> quizItem(quiz, studentId)).filter(Objects::nonNull).toList();
            List<TeacherGradebookItemResponse> examItems = exams.stream()
                    .map(exam -> examItem(exam, studentId, examResults.get(exam.getId())))
                    .filter(Objects::nonNull)
                    .toList();
            List<TeacherGradebookItemResponse> allItems = new ArrayList<>();
            allItems.addAll(assignmentItems); allItems.addAll(quizItems); allItems.addAll(examItems);
            List<TeacherGradebookItemResponse> returnedItems = allItems.stream()
                    .filter(item -> !"MISSING".equalsIgnoreCase(item.getStatus()))
                    .toList();
            BigDecimal total = returnedItems.stream().map(TeacherGradebookItemResponse::getScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal possible = returnedItems.stream().map(TeacherGradebookItemResponse::getMaxScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal percentage = possible.signum() == 0 ? null
                    : total.multiply(BigDecimal.valueOf(100)).divide(possible, 2, RoundingMode.HALF_UP);
            StudentDirectoryProfileResponse profile = profiles.get(studentId);
            String letterGrade = percentage == null ? null : gradeFor(percentage);
            return TeacherGradebookStudentResponse.builder()
                    .studentId(studentId)
                    .studentNumber(profile == null ? null : profile.getStudentNumber())
                    .studentName(profile == null || profile.getName() == null ? "Unknown Student" : profile.getName())
                    .email(profile == null ? null : profile.getEmail())
                    .assignments(assignmentItems)
                    .quizzes(quizItems)
                    .exams(examItems)
                    .assignmentScores(assignmentItems)
                    .quizScores(quizItems)
                    .examScores(examItems)
                    .total(total.setScale(2, RoundingMode.HALF_UP))
                    .percentage(percentage)
                    .grade(letterGrade)
                    .overallPercentage(percentage)
                    .letterGrade(letterGrade)
                    .status(percentage == null ? "NO_RESULTS"
                            : percentage.compareTo(BigDecimal.valueOf(60)) < 0 ? "AT_RISK" : "ON_TRACK")
                    .build();
        }).toList();
        List<BigDecimal> gradedPercentages = students.stream()
                .map(TeacherGradebookStudentResponse::getOverallPercentage)
                .filter(Objects::nonNull)
                .toList();
        BigDecimal averagePercentage = gradedPercentages.isEmpty() ? null
                : gradedPercentages.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(gradedPercentages.size()), 2, RoundingMode.HALF_UP);
        BigDecimal highestPercentage = gradedPercentages.stream()
                .max(BigDecimal::compareTo)
                .orElse(null);
        long atRiskCount = students.stream().filter(student -> "AT_RISK".equals(student.getStatus())).count();

        TeacherGradebookResponse data = TeacherGradebookResponse.builder()
                .courseId(courseId)
                .courseName(course.getCourseName())
                .gradingScale(GRADING_SCALE)
                .students(students)
                .summary(TeacherGradebookSummaryResponse.builder()
                        .studentCount(students.size())
                        .averagePercentage(averagePercentage)
                        .highestPercentage(highestPercentage)
                        .atRiskCount(atRiskCount)
                        .build())
                .build();
        return ApiResponse.<TeacherGradebookResponse>builder().statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK).message("Teacher gradebook fetched successfully").data(data).build();
    }

    private TeacherGradebookItemResponse assignmentItem(AssignmentDashboardResponse assignment,
                                                        List<SubmissionDashboardResponse> submissions, UUID studentId) {
        SubmissionDashboardResponse selected = submissions.stream()
                .filter(submission -> studentId.equals(submission.getStudentId()) && submission.getMarks() != null)
                .max(Comparator.comparing(SubmissionDashboardResponse::getAttemptNumber,
                                Comparator.nullsFirst(Integer::compareTo))
                        .thenComparing(SubmissionDashboardResponse::getSubmittedAt,
                                Comparator.nullsFirst(LocalDateTime::compareTo)))
                .orElse(null);
        BigDecimal maxMarks = assignment.getMaxMarks() == null ? null : BigDecimal.valueOf(assignment.getMaxMarks());
        if (maxMarks == null || maxMarks.signum() <= 0) return null;
        return item(assignment.getId(), assignment.getTitle(),
                selected == null ? BigDecimal.ZERO : selected.getMarks(),
                maxMarks, selected == null ? "MISSING" : selected.getStatus(), selected == null ? null
                        : selected.getGradedAt() != null ? selected.getGradedAt()
                        : selected.getUpdatedAt() != null ? selected.getUpdatedAt() : selected.getSubmittedAt());
    }

    private TeacherGradebookItemResponse quizItem(QuizGradebookResponse quiz, UUID studentId) {
        QuizGradebookResultResponse result = quiz.getResults() == null ? null : quiz.getResults().stream()
                .filter(item -> studentId.equals(item.getStudentId()) && item.getScore() != null)
                .findFirst().orElse(null);
        if (quiz.getTotalMarks() == null || quiz.getTotalMarks().signum() <= 0) return null;
        return item(quiz.getQuizId(), quiz.getTitle(), result == null ? BigDecimal.ZERO : result.getScore(),
                quiz.getTotalMarks(), result == null ? "MISSING" : "GRADED",
                result == null ? null : result.getSubmittedAt());
    }

    private TeacherGradebookItemResponse examItem(
            ExamDashboardResponse exam, UUID studentId, ExamResultSheetResponse sheet) {
        ExamResultResponse result = sheet == null || sheet.getResults() == null ? null : sheet.getResults().stream()
                .filter(item -> studentId.equals(item.getStudentId()) && item.getMarksObtained() != null).findFirst().orElse(null);
        BigDecimal max = exam.getTotalMarks() != null ? exam.getTotalMarks()
                : sheet == null ? null : sheet.getTotalMarks();
        if (max == null || max.signum() <= 0) return null;
        return item(exam.getId(), exam.getTitle(),
                result == null ? BigDecimal.ZERO : result.getMarksObtained(), max,
                result == null ? "MISSING" : result.getGrade(), result == null ? null : result.getUpdatedAt());
    }

    private TeacherGradebookItemResponse item(UUID id, String title, BigDecimal score, BigDecimal max, String status,
                                              LocalDateTime updatedAt) {
        return TeacherGradebookItemResponse.builder().assessmentId(id).title(title).score(score).maxScore(max)
                .percentage(score.multiply(BigDecimal.valueOf(100)).divide(max, 2, RoundingMode.HALF_UP))
                .status(status).marksObtained(score).maxMarks(max).updatedAt(updatedAt).build();
    }

    private String gradeFor(BigDecimal percentage) {
        if (percentage.compareTo(BigDecimal.valueOf(90)) >= 0) return "A";
        if (percentage.compareTo(BigDecimal.valueOf(80)) >= 0) return "B";
        if (percentage.compareTo(BigDecimal.valueOf(70)) >= 0) return "C";
        if (percentage.compareTo(BigDecimal.valueOf(60)) >= 0) return "D";
        return "F";
    }

    private List<UUID> enrolledStudentIds(UUID courseId, String auth) {
        ApiResponse<List<UUID>> response = entrollmentServiceClient.getEnrolledStudentIdsByCourse(courseId, auth);
        return response == null || response.getData() == null ? List.of() : response.getData();
    }

    private Map<UUID, StudentDirectoryProfileResponse> studentProfiles(List<UUID> ids, String auth) {
        if (ids.isEmpty()) return Map.of();
        ApiResponse<List<StudentDirectoryProfileResponse>> response = userServiceClient.getStudentsByIds(ids, auth);
        if (response == null || response.getData() == null) return Map.of();
        return response.getData().stream().filter(profile -> profile.getId() != null)
                .collect(Collectors.toMap(StudentDirectoryProfileResponse::getId, profile -> profile, (a, b) -> a));
    }

    private List<AssignmentDashboardResponse> assignments(UUID courseId, String auth) {
        ApiResponse<List<AssignmentDashboardResponse>> response = assignmentDashboardClient.getAssignments(courseId, auth);
        return response == null || response.getData() == null ? List.of() : response.getData().stream()
                .filter(assignment -> "CLOSED".equals(assignment.getStatus()))
                .toList();
    }

    private List<SubmissionDashboardResponse> submissions(UUID assignmentId, String auth) {
        ApiResponse<List<SubmissionDashboardResponse>> response = assignmentDashboardClient.getSubmissions(assignmentId, auth);
        return response == null || response.getData() == null ? List.of() : response.getData();
    }

    private List<QuizGradebookResponse> quizzes(UUID courseId, String auth) {
        ApiResponse<List<QuizGradebookResponse>> response = quizGradebookClient.getQuizzes(courseId, auth);
        return response == null || response.getData() == null ? List.of() : response.getData();
    }

    private List<ExamDashboardResponse> exams(UUID courseId, String auth) {
        ApiResponse<List<ExamDashboardResponse>> response = examDashboardClient.getExams(courseId, auth);
        return response == null || response.getData() == null ? List.of() : response.getData().stream()
                .filter(exam -> "MARKING".equals(exam.getStatus()) || "COMPLETED".equals(exam.getStatus()))
                .toList();
    }

    private ExamResultSheetResponse examResults(UUID examId, String auth) {
        ApiResponse<ExamResultSheetResponse> response = examDashboardClient.getExamResults(examId, auth);
        return response == null ? null : response.getData();
    }
}
