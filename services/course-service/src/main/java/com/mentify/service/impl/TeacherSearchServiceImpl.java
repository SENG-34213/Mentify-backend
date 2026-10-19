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

    private static final Set<String> SUPPORTED_TYPES = Set.of("all", "courses", "students", "assignments", "quizzes", "exams");

    private final CourseRepository courseRepository;
    private final TeacherCourseAccessGuard teacherCourseAccessGuard;
    private final EntrollmentServiceClient enrollmentClient;
    private final UserServiceClient userServiceClient;
    private final AssignmentDashboardClient assignmentClient;
    private final QuizGradebookClient quizClient;
    private final ExamDashboardClient examClient;

    @Override
    public ApiResponse<List<TeacherSearchResultResponse>> search(
            String query, String type, String authorizationHeader) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String normalizedType = type == null || type.isBlank() ? "all" : type.trim().toLowerCase(Locale.ROOT);
        if (normalizedQuery.length() < 2) {
            throw new IllegalArgumentException("q must contain at least 2 characters");
        }
        if (!SUPPORTED_TYPES.contains(normalizedType)) {
            throw new IllegalArgumentException("Unsupported search type: " + type);
        }

        List<Course> courses = visibleCourses();
        courses.forEach(course -> {
            if (teacherCourseAccessGuard.isTeacherAuthenticated()) {
                teacherCourseAccessGuard.assertTeacherOwnsCourse(course);
            }
        });

        Map<String, TeacherSearchResultResponse> results = new LinkedHashMap<>();
        if (matchesType(normalizedType, "courses")) searchCourses(courses, normalizedQuery, results);
        if (matchesType(normalizedType, "students")) searchStudents(courses, normalizedQuery, authorizationHeader, results);
        for (Course course : courses) {
            if (matchesType(normalizedType, "assignments")) searchAssignments(course, normalizedQuery, authorizationHeader, results);
            if (matchesType(normalizedType, "quizzes")) searchQuizzes(course, normalizedQuery, authorizationHeader, results);
            if (matchesType(normalizedType, "exams")) searchExams(course, normalizedQuery, authorizationHeader, results);
        }

        return ApiResponse.<List<TeacherSearchResultResponse>>builder()
                .statusCode(HttpStatus.OK.value()).status(HttpStatus.OK)
                .message("Teacher search completed successfully")
                .data(new ArrayList<>(results.values())).build();
    }

    private List<Course> visibleCourses() {
        if (teacherCourseAccessGuard.isTeacherAuthenticated()) {
            return courseRepository.findAllByAssignedTeacherIdOrderByCreatedAtDesc(
                    teacherCourseAccessGuard.getAuthenticatedTeacherId());
        }
        return courseRepository.findAll();
    }

    private void searchCourses(List<Course> courses, String query,
                               Map<String, TeacherSearchResultResponse> results) {
        courses.stream().filter(course -> contains(course.getCourseName(), query)
                        || contains(course.getSubject(), query)
                        || contains(course.getCourseDescription(), query))
                .forEach(course -> add(results, TeacherSearchResultResponse.builder()
                        .id(course.getId()).type("courses").title(course.getCourseName())
                        .subtitle(course.getSubject()).courseId(course.getId()).build()));
    }

    private void searchStudents(List<Course> courses, String query, String auth,
                                Map<String, TeacherSearchResultResponse> results) {
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
                        .id(profile.getId()).type("students").title(profile.getName())
                        .subtitle(profile.getEmail()).build()));
    }

    private void searchAssignments(Course course, String query, String auth,
                                   Map<String, TeacherSearchResultResponse> results) {
        for (AssignmentDashboardResponse assignment : data(assignmentClient.getAssignments(course.getId(), auth))) {
            if (contains(assignment.getTitle(), query)) add(results, TeacherSearchResultResponse.builder()
                    .id(assignment.getId()).type("assignments").title(assignment.getTitle())
                    .subtitle(course.getCourseName()).courseId(course.getId()).build());
        }
    }

    private void searchQuizzes(Course course, String query, String auth,
                               Map<String, TeacherSearchResultResponse> results) {
        for (QuizGradebookResponse quiz : data(quizClient.getQuizzes(course.getId(), auth))) {
            if (contains(quiz.getTitle(), query)) add(results, TeacherSearchResultResponse.builder()
                    .id(quiz.getQuizId()).type("quizzes").title(quiz.getTitle())
                    .subtitle(course.getCourseName()).courseId(course.getId()).build());
        }
    }

    private void searchExams(Course course, String query, String auth,
                             Map<String, TeacherSearchResultResponse> results) {
        for (ExamDashboardResponse exam : data(examClient.getExams(course.getId(), auth))) {
            if (contains(exam.getTitle(), query)) add(results, TeacherSearchResultResponse.builder()
                    .id(exam.getId()).type("exams").title(exam.getTitle())
                    .subtitle(course.getCourseName()).courseId(course.getId()).build());
        }
    }

    private <T> List<T> data(ApiResponse<List<T>> response) {
        return response == null || response.getData() == null ? List.of() : response.getData();
    }

    private boolean matchesType(String requested, String candidate) {
        return "all".equals(requested) || candidate.equals(requested);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private void add(Map<String, TeacherSearchResultResponse> results, TeacherSearchResultResponse result) {
        results.putIfAbsent(result.getType() + ":" + result.getId(), result);
    }
}
