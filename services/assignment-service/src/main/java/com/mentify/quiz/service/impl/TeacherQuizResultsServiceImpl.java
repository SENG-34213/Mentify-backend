package com.mentify.quiz.service.impl;

import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.client.UserServiceClient;
import com.mentify.quiz.client.dto.StudentLookupRequest;
import com.mentify.quiz.client.dto.StudentLookupResponse;
import com.mentify.quiz.dto.response.TeacherQuizResultsResponse;
import com.mentify.quiz.entity.Quiz;
import com.mentify.quiz.entity.QuizAttempt;
import com.mentify.quiz.enums.AttemptStatus;
import com.mentify.quiz.exception.QuizNotFoundException;
import com.mentify.quiz.repository.QuizAttemptRepository;
import com.mentify.quiz.repository.QuizRepository;
import com.mentify.quiz.service.CourseQuizAuthorizationService;
import com.mentify.quiz.service.TeacherQuizResultsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherQuizResultsServiceImpl implements TeacherQuizResultsService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final UserServiceClient userServiceClient;
    private final CourseQuizAuthorizationService courseAuthorizationService;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<TeacherQuizResultsResponse> getResults(UUID quizId, String authorizationHeader) {
        Quiz quiz = quizRepository.findByIdAndIsActiveTrue(quizId)
                .orElseThrow(() -> new QuizNotFoundException(quizId));
        courseAuthorizationService.assertTeacherAssignedToCourse(quiz.getCourseId(), authorizationHeader);

        Map<UUID, QuizAttempt> latestAttempts = quizAttemptRepository
                .findByQuizIdInAndStatusAndIsActiveTrue(List.of(quizId), AttemptStatus.SUBMITTED)
                .stream()
                .filter(attempt -> attempt.getStudentId() != null)
                .sorted(Comparator.comparing(QuizAttempt::getSubmittedAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(QuizAttempt::getAttemptNumber,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toMap(
                        QuizAttempt::getStudentId,
                        Function.identity(),
                        (first, ignored) -> first
                ));

        Map<UUID, String> studentNames = studentNames(latestAttempts.keySet().stream().toList(), authorizationHeader);
        List<TeacherQuizResultsResponse.TeacherQuizResultResponse> results = latestAttempts.values().stream()
                .sorted(Comparator.comparing(QuizAttempt::getSubmittedAt,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(QuizAttempt::getStudentId))
                .map(attempt -> result(quiz, attempt, studentNames))
                .toList();

        TeacherQuizResultsResponse data = TeacherQuizResultsResponse.builder()
                .quizId(quiz.getId())
                .totalMarks(quiz.getTotalMarks())
                .results(results)
                .build();
        return ApiResponse.<TeacherQuizResultsResponse>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("Quiz results fetched successfully")
                .data(data)
                .build();
    }

    private Map<UUID, String> studentNames(List<UUID> studentIds, String authorizationHeader) {
        if (studentIds.isEmpty()) {
            return Map.of();
        }
        var response = userServiceClient.lookupStudents(new StudentLookupRequest(studentIds), authorizationHeader);
        if (response == null || response.getData() == null) {
            return Map.of();
        }
        return response.getData().stream()
                .filter(student -> student.getStudentId() != null)
                .collect(Collectors.toMap(
                        StudentLookupResponse::getStudentId,
                        student -> studentName(student),
                        (first, ignored) -> first
                ));
    }

    private String studentName(StudentLookupResponse student) {
        String firstName = student.getFirstName() == null ? "" : student.getFirstName().trim();
        String lastName = student.getLastName() == null ? "" : student.getLastName().trim();
        String name = (firstName + " " + lastName).trim();
        return name.isBlank() ? "Unknown Student" : name;
    }

    private TeacherQuizResultsResponse.TeacherQuizResultResponse result(
            Quiz quiz, QuizAttempt attempt, Map<UUID, String> studentNames) {
        BigDecimal percentage = attempt.getPercentage();
        if (percentage == null && quiz.getTotalMarks() != null && quiz.getTotalMarks().signum() > 0
                && attempt.getScore() != null) {
            percentage = attempt.getScore().multiply(BigDecimal.valueOf(100))
                    .divide(quiz.getTotalMarks(), 2, RoundingMode.HALF_UP);
        }
        return TeacherQuizResultsResponse.TeacherQuizResultResponse.builder()
                .studentId(attempt.getStudentId())
                .studentName(studentNames.getOrDefault(attempt.getStudentId(), "Unknown Student"))
                .marksObtained(attempt.getScore())
                .percentage(percentage)
                .passed(quiz.getPassMark() != null && attempt.getScore() != null
                        && attempt.getScore().compareTo(quiz.getPassMark()) >= 0)
                .submittedAt(attempt.getSubmittedAt())
                .build();
    }
}
