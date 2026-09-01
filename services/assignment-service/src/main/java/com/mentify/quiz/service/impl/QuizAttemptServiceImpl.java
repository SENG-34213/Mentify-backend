package com.mentify.quiz.service.impl;

import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.client.EnrollmentServiceClient;
import com.mentify.quiz.client.UserServiceClient;
import com.mentify.quiz.dto.request.SaveStudentAnswerRequest;
import com.mentify.quiz.dto.response.StartQuizAttemptResponse;
import com.mentify.quiz.dto.response.StudentAnswerResponse;
import com.mentify.quiz.dto.response.SubmitQuizResponse;
import com.mentify.quiz.entity.QuestionOption;
import com.mentify.quiz.entity.Quiz;
import com.mentify.quiz.entity.QuizAttempt;
import com.mentify.quiz.entity.QuizQuestion;
import com.mentify.quiz.entity.StudentAnswer;
import com.mentify.quiz.enums.AttemptStatus;
import com.mentify.quiz.enums.QuizStatus;
import com.mentify.quiz.exception.ActiveQuizAttemptExistsException;
import com.mentify.quiz.exception.InvalidQuestionOptionsException;
import com.mentify.quiz.exception.OptionNotFoundException;
import com.mentify.quiz.exception.QuestionNotFoundException;
import com.mentify.quiz.exception.QuizAlreadySubmittedException;
import com.mentify.quiz.exception.QuizAttemptLimitExceededException;
import com.mentify.quiz.exception.QuizAttemptNotFoundException;
import com.mentify.quiz.exception.QuizNotFoundException;
import com.mentify.quiz.exception.QuizNotPublishedException;
import com.mentify.quiz.exception.QuizUnavailableException;
import com.mentify.quiz.exception.StudentNotEnrolledException;
import com.mentify.quiz.exception.StudentNotFoundException;
import com.mentify.quiz.exception.UnauthorizedQuizAccessException;
import com.mentify.quiz.repository.QuestionOptionRepository;
import com.mentify.quiz.repository.QuizAttemptRepository;
import com.mentify.quiz.repository.QuizQuestionRepository;
import com.mentify.quiz.repository.QuizRepository;
import com.mentify.quiz.repository.StudentAnswerRepository;
import com.mentify.quiz.security.CurrentUserService;
import com.mentify.quiz.service.QuizAttemptService;
import com.mentify.quiz.service.QuizGradingService;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuizAttemptServiceImpl implements QuizAttemptService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final QuizGradingService quizGradingService;
    private final UserServiceClient userServiceClient;
    private final EnrollmentServiceClient enrollmentServiceClient;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public ApiResponse<StartQuizAttemptResponse> startAttempt(UUID quizId, String authorizationHeader) {
        UUID studentId = currentUserService.getCurrentUserId();
        assertStudentExists(studentId, authorizationHeader);

        Quiz quiz = quizRepository.findByIdAndIsActiveTrue(quizId)
                .orElseThrow(() -> new QuizNotFoundException(quizId));

        if (quiz.getStatus() != QuizStatus.PUBLISHED) {
            throw new QuizNotPublishedException();
        }

        validateQuizAvailable(quiz);
        assertStudentEnrolled(studentId, quiz.getCourseId(), authorizationHeader);

        long attemptCount = quizAttemptRepository.countByQuizIdAndStudentIdAndIsActiveTrue(quizId, studentId);
        if (attemptCount >= quiz.getMaxAttempts()) {
            throw new QuizAttemptLimitExceededException();
        }

        if (quizAttemptRepository.existsByQuizIdAndStudentIdAndStatusAndIsActiveTrue(
                quizId,
                studentId,
                AttemptStatus.IN_PROGRESS
        )) {
            throw new ActiveQuizAttemptExistsException();
        }

        QuizAttempt attempt = QuizAttempt.builder()
                .quizId(quizId)
                .studentId(studentId)
                .attemptNumber((int) attemptCount + 1)
                .startedAt(LocalDateTime.now())
                .status(AttemptStatus.IN_PROGRESS)
                .score(BigDecimal.ZERO)
                .percentage(BigDecimal.ZERO)
                .build();

        QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);
        StartQuizAttemptResponse response = StartQuizAttemptResponse.builder()
                .attemptId(savedAttempt.getId())
                .quizId(savedAttempt.getQuizId())
                .studentId(savedAttempt.getStudentId())
                .attemptNumber(savedAttempt.getAttemptNumber())
                .status(savedAttempt.getStatus())
                .startedAt(savedAttempt.getStartedAt())
                .build();

        return response(HttpStatus.CREATED, "Quiz attempt started successfully", response);
    }

    @Override
    @Transactional
    public ApiResponse<StudentAnswerResponse> saveAnswer(UUID attemptId, SaveStudentAnswerRequest request) {
        UUID studentId = currentUserService.getCurrentUserId();
        QuizAttempt attempt = quizAttemptRepository.findByIdAndStudentIdAndIsActiveTrue(attemptId, studentId)
                .orElseThrow(() -> new QuizAttemptNotFoundException(attemptId));

        assertAttemptInProgress(attempt);

        QuizQuestion question = quizQuestionRepository.findByQuiz_IdAndIdAndIsActiveTrue(
                        attempt.getQuizId(),
                        request.getQuestionId()
                )
                .orElseThrow(() -> new QuestionNotFoundException(request.getQuestionId()));

        QuestionOption option = questionOptionRepository.findByIdAndQuestion_IdAndIsActiveTrue(
                        request.getSelectedOptionId(),
                        question.getId()
                )
                .orElseThrow(() -> new OptionNotFoundException(request.getSelectedOptionId()));

        StudentAnswer answer = studentAnswerRepository
                .findByAttempt_IdAndQuestionIdAndIsActiveTrue(attemptId, question.getId())
                .orElseGet(() -> StudentAnswer.builder()
                        .attempt(attempt)
                        .questionId(question.getId())
                        .build());

        answer.setSelectedOptionId(option.getId());
        answer.setCorrect(null);
        answer.setAwardedMarks(null);
        answer.setAnsweredAt(LocalDateTime.now());

        StudentAnswer savedAnswer = studentAnswerRepository.save(answer);
        StudentAnswerResponse response = StudentAnswerResponse.builder()
                .id(savedAnswer.getId())
                .attemptId(attempt.getId())
                .questionId(savedAnswer.getQuestionId())
                .selectedOptionId(savedAnswer.getSelectedOptionId())
                .answeredAt(savedAnswer.getAnsweredAt())
                .build();

        return response(HttpStatus.OK, "Answer saved successfully", response);
    }

    @Override
    @Transactional
    public ApiResponse<SubmitQuizResponse> submitAttempt(UUID attemptId) {
        UUID studentId = currentUserService.getCurrentUserId();
        QuizAttempt attempt = quizAttemptRepository.findWithAnswersByIdAndIsActiveTrue(attemptId)
                .orElseThrow(() -> new QuizAttemptNotFoundException(attemptId));

        if (!attempt.getStudentId().equals(studentId)) {
            throw new UnauthorizedQuizAccessException("Student cannot access another student's attempt");
        }

        assertAttemptInProgress(attempt);

        Quiz quiz = quizRepository.findByIdAndIsActiveTrue(attempt.getQuizId())
                .orElseThrow(() -> new QuizNotFoundException(attempt.getQuizId()));
        List<QuizQuestion> questions = quizQuestionRepository
                .findByQuiz_IdAndIsActiveTrueOrderByQuestionOrderAsc(quiz.getId());
        if (questions.isEmpty()) {
            throw new InvalidQuestionOptionsException("Quiz has no questions");
        }

        List<StudentAnswer> answers = studentAnswerRepository.findByAttempt_IdAndIsActiveTrue(attemptId);
        attempt.setSubmittedAt(LocalDateTime.now());
        SubmitQuizResponse result = quizGradingService.grade(quiz, attempt, questions, answers);
        attempt.setStatus(AttemptStatus.SUBMITTED);

        studentAnswerRepository.saveAll(answers);
        quizAttemptRepository.save(attempt);

        result.setSubmittedAt(attempt.getSubmittedAt());
        return response(HttpStatus.OK, "Quiz submitted successfully", result);
    }

    private void assertAttemptInProgress(QuizAttempt attempt) {
        if (attempt.getStatus() == AttemptStatus.SUBMITTED) {
            throw new QuizAlreadySubmittedException();
        }
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new QuizUnavailableException("Quiz attempt is not in progress");
        }
    }

    private void validateQuizAvailable(Quiz quiz) {
        LocalDateTime now = LocalDateTime.now();
        if (quiz.getStartTime() != null && now.isBefore(quiz.getStartTime())) {
            throw new QuizUnavailableException("Quiz is not available yet");
        }
        if (quiz.getEndTime() != null && now.isAfter(quiz.getEndTime())) {
            throw new QuizUnavailableException("Quiz is closed");
        }
    }

    private void assertStudentExists(UUID studentId, String authorizationHeader) {
        try {
            if (!userServiceClient.isStudentExists(studentId, authorizationHeader)) {
                throw new StudentNotFoundException(studentId);
            }
        } catch (FeignException.NotFound ex) {
            throw new StudentNotFoundException(studentId);
        } catch (FeignException ex) {
            throw new IllegalStateException("Failed to validate student", ex);
        }
    }

    private void assertStudentEnrolled(UUID studentId, UUID courseId, String authorizationHeader) {
        try {
            if (!enrollmentServiceClient.isStudentEnrolledInCourse(studentId, courseId, authorizationHeader)) {
                throw new StudentNotEnrolledException();
            }
        } catch (FeignException.Forbidden ex) {
            throw new StudentNotEnrolledException();
        } catch (FeignException ex) {
            throw new IllegalStateException("Failed to validate enrollment", ex);
        }
    }

    private <T> ApiResponse<T> response(HttpStatus status, String message, T data) {
        return ApiResponse.<T>builder()
                .status(status)
                .statusCode(status.value())
                .message(message)
                .data(data)
                .build();
    }
}
