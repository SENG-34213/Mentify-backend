package com.mentify.service.impl;

import com.mentify.client.AssignmentDashboardClient;
import com.mentify.client.EntrollmentServiceClient;
import com.mentify.client.ExamDashboardClient;
import com.mentify.client.QuizGradebookClient;
import com.mentify.client.UserServiceClient;
import com.mentify.client.dto.AssignmentDashboardResponse;
import com.mentify.client.dto.ExamDashboardResponse;
import com.mentify.client.dto.QuizGradebookResponse;
import com.mentify.client.dto.StudentDirectoryProfileResponse;
import com.mentify.dto.TeacherSearchResultResponse;
import com.mentify.dto.TeacherSearchResponse;
import com.mentify.entity.Course;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.CourseRepository;
import com.mentify.service.TeacherSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherSearchServiceImpl implements TeacherSearchService {

    private static final Set<String> SUPPORTED_TYPES = Set.of("course", "student", "assignment", "quiz", "exam");

    private final CourseRepository courseRepository;
    private final TeacherCourseAccessGuard teacherCourseAccessGuard;
    private final EntrollmentServiceClient enrollmentClient;
    private final UserServiceClient userServiceClient;
    private final AssignmentDashboardClient assignmentClient;
    private final QuizGradebookClient quizClient;
    private final ExamDashboardClient examClient;

    @Override
    public ApiResponse<TeacherSearchResponse> search(
            String query, String types, int limit, String authorizationHeader) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalizedQuery.length() < 2) {
            throw new IllegalArgumentException("q must contain at least 2 characters");
        }
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }
        Set<String> requestedTypes = parseTypes(types);

        List<Course> courses = visibleCourses();
        courses.forEach(course -> {
            if (teacherCourseAccessGuard.isTeacherAuthenticated()) {
                teacherCourseAccessGuard.assertTeacherOwnsCourse(course);
            }
        });

        Map<String, TeacherSearchResultResponse> results = new LinkedHashMap<>();
        if (requestedTypes.contains("course")) searchCourses(courses, normalizedQuery, results, limit);
        if (requestedTypes.contains("student")) searchStudents(courses, normalizedQuery, authorizationHeader, results, limit);
        for (Course course : courses) {
            if (requestedTypes.contains("assignment")) searchAssignments(course, normalizedQuery, authorizationHeader, results, limit);
            if (requestedTypes.contains("quiz")) searchQuizzes(course, normalizedQuery, authorizationHeader, results, limit);
            if (requestedTypes.contains("exam")) searchExams(course, normalizedQuery, authorizationHeader, results, limit);
        }

        List<TeacherSearchResultResponse> bounded = results.values().stream().limit(limit).toList();
        return ApiResponse.<TeacherSearchResponse>builder()
                .statusCode(HttpStatus.OK.value()).status(HttpStatus.OK)
                .message("Teacher search completed successfully")
                .data(TeacherSearchResponse.builder().results(bounded).total(bounded.size()).build()).build();
    }

    private Set<String> parseTypes(String types) {
        if (types == null || types.isBlank()) return SUPPORTED_TYPES;
        Set<String> parsed = java.util.Arrays.stream(types.split(","))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .map(value -> value.endsWith("s") ? value.substring(0, value.length() - 1) : value)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        if (!SUPPORTED_TYPES.containsAll(parsed)) {
            throw new IllegalArgumentException("types must contain only course, student, assignment, quiz, or exam");
        }
        return parsed;
    }

    private List<Course> visibleCourses() {
        if (teacherCourseAccessGuard.isTeacherAuthenticated()) {
            return courseRepository.findAllByAssignedTeacherIdOrderByCreatedAtDesc(
                    teacherCourseAccessGuard.getAuthenticatedTeacherId());
        }
        return courseRepository.findAll();
    }

    private void searchCourses(List<Course> courses, String query,
                               Map<String, TeacherSearchResultResponse> results, int limit) {
        courses.stream().filter(course -> contains(course.getCourseName(), query)
                        || contains(course.getSubject(), query)
                        || contains(course.getCourseDescription(), query))
                .forEach(course -> add(results, TeacherSearchResultResponse.builder()
                        .id(course.getId()).type("COURSE").title(course.getCourseName())
                        .subtitle(course.getSubject()).courseId(course.getId()).navigationTarget("courses").build()));
    }

    private void searchStudents(List<Course> courses, String query, String auth,
                                Map<String, TeacherSearchResultResponse> results, int limit) {
        Map<UUID, Set<UUID>> studentCourses = new LinkedHashMap<>();
        for (Course course : courses) {
            ApiResponse<List<UUID>> response = enrollmentClient.getEnrolledStudentIdsByCourse(course.getId(), auth);
            List<UUID> ids = response == null || response.getData() == null ? List.of() : response.getData();
            ids.forEach(id -> studentCourses.computeIfAbsent(id, ignored -> new java.util.LinkedHashSet<>()).add(course.getId()));
        }
        if (studentCourses.isEmpty()) return;
        ApiResponse<List<StudentDirectoryProfileResponse>> response = userServiceClient
                .getStudentsByIds(new ArrayList<>(studentCourses.keySet()), auth);
        List<StudentDirectoryProfileResponse> profiles = response == null || response.getData() == null
                ? List.of() : response.getData();
        profiles.stream().filter(profile -> contains(profile.getName(), query)
                        || contains(profile.getEmail(), query)
                        || contains(profile.getStudentNumber(), query)
                        || contains(profile.getProgram(), query))
                .forEach(profile -> add(results, TeacherSearchResultResponse.builder()
                        .id(profile.getId()).type("STUDENT").title(profile.getName())
                        .subtitle(profile.getEmail()).navigationTarget("students").build()));
    }

    private void searchAssignments(Course course, String query, String auth,
                                   Map<String, TeacherSearchResultResponse> results, int limit) {
        for (AssignmentDashboardResponse assignment : data(assignmentClient.getAssignments(course.getId(), auth))) {
            if (contains(assignment.getTitle(), query)) add(results, TeacherSearchResultResponse.builder()
                    .id(assignment.getId()).type("ASSIGNMENT").title(assignment.getTitle())
                    .subtitle(course.getCourseName()).courseId(course.getId()).navigationTarget("assignments").build());
        }
    }

    private void searchQuizzes(Course course, String query, String auth,
                               Map<String, TeacherSearchResultResponse> results, int limit) {
        for (QuizGradebookResponse quiz : data(quizClient.getQuizzes(course.getId(), auth))) {
            if (contains(quiz.getTitle(), query)) add(results, TeacherSearchResultResponse.builder()
                    .id(quiz.getQuizId()).type("QUIZ").title(quiz.getTitle())
                    .subtitle(course.getCourseName()).courseId(course.getId()).navigationTarget("quizzes").build());
        }
    }

    private void searchExams(Course course, String query, String auth,
                               Map<String, TeacherSearchResultResponse> results, int limit) {
        for (ExamDashboardResponse exam : data(examClient.getExams(course.getId(), auth))) {
            if (contains(exam.getTitle(), query)) add(results, TeacherSearchResultResponse.builder()
                    .id(exam.getId()).type("EXAM").title(exam.getTitle())
                    .subtitle(course.getCourseName()).courseId(course.getId()).navigationTarget("exams").build());
        }
    }

    private <T> List<T> data(ApiResponse<List<T>> response) {
        return response == null || response.getData() == null ? List.of() : response.getData();
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private void add(Map<String, TeacherSearchResultResponse> results, TeacherSearchResultResponse result) {
        results.putIfAbsent(result.getType() + ":" + result.getId(), result);
    }
}
