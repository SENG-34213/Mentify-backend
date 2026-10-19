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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        Map<UUID, String> names = studentNames(studentIds, authorizationHeader);
        List<AssignmentDashboardResponse> assignments = assignments(courseId, authorizationHeader);
        Map<UUID, List<SubmissionDashboardResponse>> submissions = new LinkedHashMap<>();
        assignments.forEach(assignment -> submissions.put(assignment.getId(),
                submissions(assignment.getId(), authorizationHeader)));
        List<QuizGradebookResponse> quizzes = quizzes(courseId, authorizationHeader);
        List<ExamDashboardResponse> exams = exams(courseId, authorizationHeader);

        List<TeacherGradebookStudentResponse> students = studentIds.stream().distinct().map(studentId -> {
            List<TeacherGradebookItemResponse> assignmentItems = assignments.stream()
                    .map(assignment -> assignmentItem(assignment, submissions.getOrDefault(assignment.getId(), List.of()), studentId))
                    .filter(item -> item != null).toList();
            List<TeacherGradebookItemResponse> quizItems = quizzes.stream()
                    .map(quiz -> quizItem(quiz, studentId)).filter(item -> item != null).toList();
            List<TeacherGradebookItemResponse> examItems = exams.stream()
                    .map(exam -> examItem(exam, studentId, authorizationHeader)).filter(item -> item != null).toList();
            List<TeacherGradebookItemResponse> allItems = new ArrayList<>();
            allItems.addAll(assignmentItems); allItems.addAll(quizItems); allItems.addAll(examItems);
            BigDecimal total = allItems.stream().map(TeacherGradebookItemResponse::getScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal possible = allItems.stream().map(TeacherGradebookItemResponse::getMaxScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal percentage = possible.signum() == 0 ? BigDecimal.ZERO
                    : total.multiply(BigDecimal.valueOf(100)).divide(possible, 2, RoundingMode.HALF_UP);
            return TeacherGradebookStudentResponse.builder().studentId(studentId)
                    .studentName(names.getOrDefault(studentId, "Unknown Student"))
                    .assignments(assignmentItems).quizzes(quizItems).exams(examItems)
                    .total(total.setScale(2, RoundingMode.HALF_UP)).percentage(percentage)
                    .grade(gradeFor(percentage)).build();
        }).toList();

        TeacherGradebookResponse data = TeacherGradebookResponse.builder()
                .courseId(courseId).gradingScale(GRADING_SCALE).students(students).build();
        return ApiResponse.<TeacherGradebookResponse>builder().statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK).message("Teacher gradebook fetched successfully").data(data).build();
    }

    private TeacherGradebookItemResponse assignmentItem(AssignmentDashboardResponse assignment,
                                                        List<SubmissionDashboardResponse> submissions, UUID studentId) {
        SubmissionDashboardResponse selected = null;
        for (SubmissionDashboardResponse submission : submissions) {
            if (studentId.equals(submission.getStudentId()) && submission.getMarks() != null) selected = submission;
        }
        if (selected == null || assignment.getMaxMarks() == null || assignment.getMaxMarks() <= 0) return null;
        return item(assignment.getId(), assignment.getTitle(), selected.getMarks(),
                BigDecimal.valueOf(assignment.getMaxMarks()), selected.getStatus());
    }

    private TeacherGradebookItemResponse quizItem(QuizGradebookResponse quiz, UUID studentId) {
        if (quiz.getResults() == null) return null;
        QuizGradebookResultResponse result = quiz.getResults().stream()
                .filter(item -> studentId.equals(item.getStudentId()) && item.getScore() != null).findFirst().orElse(null);
        return result == null || quiz.getTotalMarks() == null || quiz.getTotalMarks().signum() <= 0 ? null
                : item(quiz.getQuizId(), quiz.getTitle(), result.getScore(), quiz.getTotalMarks(), "GRADED");
    }

    private TeacherGradebookItemResponse examItem(ExamDashboardResponse exam, UUID studentId, String auth) {
        ApiResponse<ExamResultSheetResponse> response = examDashboardClient.getExamResults(exam.getId(), auth);
        ExamResultSheetResponse sheet = response == null ? null : response.getData();
        if (sheet == null || sheet.getResults() == null) return null;
        ExamResultResponse result = sheet.getResults().stream()
                .filter(item -> studentId.equals(item.getStudentId()) && item.getMarksObtained() != null).findFirst().orElse(null);
        BigDecimal max = exam.getTotalMarks() != null ? exam.getTotalMarks() : sheet.getTotalMarks();
        return result == null || max == null || max.signum() <= 0 ? null
                : item(exam.getId(), exam.getTitle(), result.getMarksObtained(), max, result.getGrade());
    }

    private TeacherGradebookItemResponse item(UUID id, String title, BigDecimal score, BigDecimal max, String status) {
        return TeacherGradebookItemResponse.builder().assessmentId(id).title(title).score(score).maxScore(max)
                .percentage(score.multiply(BigDecimal.valueOf(100)).divide(max, 2, RoundingMode.HALF_UP)).status(status).build();
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

    private Map<UUID, String> studentNames(List<UUID> ids, String auth) {
        if (ids.isEmpty()) return Map.of();
        ApiResponse<List<StudentDirectoryProfileResponse>> response = userServiceClient.getStudentsByIds(ids, auth);
        if (response == null || response.getData() == null) return Map.of();
        return response.getData().stream().filter(profile -> profile.getId() != null)
                .collect(java.util.stream.Collectors.toMap(StudentDirectoryProfileResponse::getId,
                        profile -> profile.getName() == null ? "Unknown Student" : profile.getName(), (a, b) -> a));
    }

    private List<AssignmentDashboardResponse> assignments(UUID courseId, String auth) {
        ApiResponse<List<AssignmentDashboardResponse>> response = assignmentDashboardClient.getAssignments(courseId, auth);
        return response == null || response.getData() == null ? List.of() : response.getData();
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
        return response == null || response.getData() == null ? List.of() : response.getData();
    }
}
