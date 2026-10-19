package com.mentify.quiz.service.impl;

import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.dto.response.TeacherGradebookQuizResponse;
import com.mentify.quiz.dto.response.TeacherGradebookQuizResultResponse;
import com.mentify.quiz.entity.Quiz;
import com.mentify.quiz.enums.AttemptStatus;
import com.mentify.quiz.enums.QuizStatus;
import com.mentify.quiz.repository.QuizAttemptRepository;
import com.mentify.quiz.repository.QuizRepository;
import com.mentify.quiz.security.CurrentUserService;
import com.mentify.quiz.service.TeacherGradebookQuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherGradebookQuizServiceImpl implements TeacherGradebookQuizService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final CurrentUserService currentUserService;

    @Override
    public ApiResponse<List<TeacherGradebookQuizResponse>> getQuizzes(UUID courseId) {
        List<Quiz> quizzes = quizRepository.findByCourseIdAndIsActiveTrueOrderByStartTimeAsc(courseId)
                .stream()
                .filter(quiz -> quiz.getStatus() == QuizStatus.PUBLISHED)
                .filter(quiz -> !currentUserService.hasAnyRole("TEACHER")
                        || quiz.getTeacherId().equals(currentUserService.getCurrentUserId()))
                .toList();

        List<UUID> quizIds = quizzes.stream().map(Quiz::getId).toList();
        Map<UUID, List<com.mentify.quiz.entity.QuizAttempt>> attemptsByQuiz = quizIds.isEmpty()
                ? Map.of()
                : quizAttemptRepository.findByQuizIdInAndStatusAndIsActiveTrue(quizIds, AttemptStatus.SUBMITTED)
                .stream().collect(Collectors.groupingBy(com.mentify.quiz.entity.QuizAttempt::getQuizId));

        List<TeacherGradebookQuizResponse> response = quizzes.stream().map(quiz ->
                TeacherGradebookQuizResponse.builder()
                        .quizId(quiz.getId())
                        .title(quiz.getTitle())
                        .totalMarks(quiz.getTotalMarks())
                        .results(attemptsByQuiz.getOrDefault(quiz.getId(), List.of()).stream()
                                .collect(Collectors.toMap(
                                        com.mentify.quiz.entity.QuizAttempt::getStudentId,
                                        Function.identity(),
                                        (first, latest) -> latest))
                                .values().stream()
                                .map(attempt -> TeacherGradebookQuizResultResponse.builder()
                                        .studentId(attempt.getStudentId())
                                        .score(attempt.getScore())
                                        .percentage(attempt.getPercentage())
                                        .build())
                                .toList())
                        .build()).toList();

        return ApiResponse.<List<TeacherGradebookQuizResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .message("Teacher gradebook quizzes fetched successfully")
                .data(response)
                .build();
    }
}
