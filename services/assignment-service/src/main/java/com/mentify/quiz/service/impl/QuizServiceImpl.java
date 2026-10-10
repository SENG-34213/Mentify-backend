package com.mentify.quiz.service.impl;

import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.client.EnrollmentServiceClient;
import com.mentify.quiz.client.dto.CourseLookupResponse;
import com.mentify.quiz.dto.request.CreateQuizRequest;
import com.mentify.quiz.dto.request.UpdateQuizRequest;
import com.mentify.quiz.dto.response.QuizResponse;
import com.mentify.quiz.dto.response.StudentQuizResponse;
import com.mentify.quiz.entity.QuestionOption;
import com.mentify.quiz.entity.Quiz;
import com.mentify.quiz.entity.QuizQuestion;
import com.mentify.quiz.enums.QuestionType;
import com.mentify.quiz.enums.QuizCreationMethod;
import com.mentify.quiz.enums.QuizStatus;
import com.mentify.quiz.exception.InvalidQuestionOptionsException;
import com.mentify.quiz.exception.QuizNotFoundException;
import com.mentify.quiz.exception.StudentNotEnrolledException;
import com.mentify.quiz.exception.UnauthorizedQuizAccessException;
import com.mentify.quiz.mapper.QuizMapper;
import com.mentify.quiz.repository.QuizQuestionRepository;
import com.mentify.quiz.repository.QuizRepository;
import com.mentify.quiz.security.CurrentUserService;
import com.mentify.quiz.service.CourseQuizAuthorizationService;
import com.mentify.quiz.service.QuizService;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final EnrollmentServiceClient enrollmentServiceClient;
    private final CurrentUserService currentUserService;
    private final CourseQuizAuthorizationService courseQuizAuthorizationService;

    @Override
    @Transactional
    public ApiResponse<QuizResponse> createQuiz(CreateQuizRequest request, String authorizationHeader) {
        CourseLookupResponse course = courseQuizAuthorizationService.assertCanCreateQuizForCourse(
                request.getCourseId(),
                authorizationHeader
        );
        validateQuizTimes(request.getStartTime(), request.getEndTime());

        Quiz quiz = Quiz.builder()
                .courseId(request.getCourseId())
                .teacherId(courseQuizAuthorizationService.resolveTeacherId(course))
                .title(request.getTitle().trim())
                .description(trimToNull(request.getDescription()))
                .durationMinutes(request.getDurationMinutes())
                .totalMarks(BigDecimal.ZERO)
                .creationMethod(resolveCreationMethod(request))
                .passMark(request.getPassMark())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .maxAttempts(request.getMaxAttempts())
                .status(QuizStatus.DRAFT)
                .showResultImmediately(request.getShowResultImmediately() != null
                        ? request.getShowResultImmediately()
                        : Boolean.TRUE)
                .build();

        Quiz savedQuiz = quizRepository.save(quiz);
        return response(HttpStatus.CREATED, "Quiz created successfully",
                QuizMapper.toTeacherQuizResponse(savedQuiz, List.of()));
    }

    @Override
    @Transactional
    public ApiResponse<QuizResponse> updateQuiz(UUID quizId, UpdateQuizRequest request, String authorizationHeader) {
        Quiz quiz = getQuizOrThrow(quizId);
        assertCanManageQuiz(quiz);
        validateQuizTimes(request.getStartTime(), request.getEndTime());

        quiz.setTitle(request.getTitle().trim());
        quiz.setDescription(trimToNull(request.getDescription()));
        quiz.setDurationMinutes(request.getDurationMinutes());
        quiz.setPassMark(request.getPassMark());
        quiz.setStartTime(request.getStartTime());
        quiz.setEndTime(request.getEndTime());
        quiz.setMaxAttempts(request.getMaxAttempts());
        quiz.setShowResultImmediately(request.getShowResultImmediately() != null
                ? request.getShowResultImmediately()
                : Boolean.TRUE);

        courseQuizAuthorizationService.getCourseOrThrow(quiz.getCourseId(), authorizationHeader);

        List<QuizQuestion> questions = quizQuestionRepository.findByQuiz_IdAndIsActiveTrueOrderByQuestionOrderAsc(quizId);
        return response(HttpStatus.OK, "Quiz updated successfully",
                QuizMapper.toTeacherQuizResponse(quizRepository.save(quiz), questions));
    }

    @Override
    @Transactional
    public ApiResponse<QuizResponse> publishQuiz(UUID quizId, String authorizationHeader) {
        Quiz quiz = getQuizOrThrow(quizId);

        assertCanManageQuiz(quiz);
        assertDraft(quiz);
        courseQuizAuthorizationService.getCourseOrThrow(quiz.getCourseId(), authorizationHeader);

        List<QuizQuestion> questions = quizQuestionRepository.findByQuiz_IdAndIsActiveTrueOrderByQuestionOrderAsc(quizId);
        validatePublishable(questions);

        quiz.setTotalMarks(calculateTotalMarks(questions));
        if (quiz.getPassMark().compareTo(quiz.getTotalMarks()) > 0) {
            throw new InvalidQuestionOptionsException("Pass mark cannot be greater than total marks");
        }
        quiz.setStatus(QuizStatus.PUBLISHED);

        return response(HttpStatus.OK, "Quiz published successfully",
                QuizMapper.toTeacherQuizResponse(quizRepository.save(quiz), questions));
    }

    @Override
    public ApiResponse<QuizResponse> getTeacherQuiz(UUID quizId) {
        Quiz quiz = getQuizOrThrow(quizId);
        assertCanManageQuiz(quiz);
        List<QuizQuestion> questions = quizQuestionRepository.findByQuiz_IdAndIsActiveTrueOrderByQuestionOrderAsc(quizId);

        return response(HttpStatus.OK, "Quiz fetched successfully",
                QuizMapper.toTeacherQuizResponse(quiz, questions));
    }

    @Override
    public ApiResponse<List<QuizResponse>> getTeacherQuizzes() {
        List<QuizResponse> quizzes = quizRepository
                .findByTeacherIdAndIsActiveTrueOrderByCreatedAtDesc(currentUserService.getCurrentUserId())
                .stream()
                .map(quiz -> QuizMapper.toTeacherQuizResponse(
                        quiz,
                        quizQuestionRepository.findByQuiz_IdAndIsActiveTrueOrderByQuestionOrderAsc(quiz.getId())
                ))
                .toList();
        return response(HttpStatus.OK, "Teacher quizzes fetched successfully", quizzes);
    }

    @Override
    @Transactional
    public ApiResponse<Object> deleteQuiz(UUID quizId) {
        Quiz quiz = getQuizOrThrow(quizId);
        assertCanManageQuiz(quiz);
        quiz.setActive(false);
        quizRepository.save(quiz);
        return response(HttpStatus.OK, "Quiz deleted successfully", null);
    }

    @Override
    public ApiResponse<List<StudentQuizResponse>> getPublishedQuizzesByCourse(UUID courseId, String authorizationHeader) {
        UUID studentId = currentUserService.getCurrentUserId();
        assertStudentEnrolled(studentId, courseId, authorizationHeader);

        List<StudentQuizResponse> quizzes = quizRepository
                .findByCourseIdAndStatusAndIsActiveTrueOrderByStartTimeAsc(courseId, QuizStatus.PUBLISHED)
                .stream()
                .map(quiz -> QuizMapper.toStudentQuizResponse(quiz, null))
                .toList();

        return response(HttpStatus.OK, "Quizzes fetched successfully", quizzes);
    }

    @Override
    public ApiResponse<StudentQuizResponse> getStudentQuiz(UUID quizId, String authorizationHeader) {
        Quiz quiz = getQuizOrThrow(quizId);
        if (quiz.getStatus() != QuizStatus.PUBLISHED) {
            throw new QuizNotFoundException(quizId);
        }

        UUID studentId = currentUserService.getCurrentUserId();
        assertStudentEnrolled(studentId, quiz.getCourseId(), authorizationHeader);

        List<QuizQuestion> questions = quizQuestionRepository.findByQuiz_IdAndIsActiveTrueOrderByQuestionOrderAsc(quizId);
        return response(HttpStatus.OK, "Quiz fetched successfully", QuizMapper.toStudentQuizResponse(quiz, questions));
    }

    private void validatePublishable(List<QuizQuestion> questions) {
        if (questions.isEmpty()) {
            throw new InvalidQuestionOptionsException("Quiz must contain at least one question before publishing");
        }

        questions.forEach(question -> {
            if (question.getQuestionType() != QuestionType.MULTIPLE_CHOICE_SINGLE_ANSWER) {
                throw new InvalidQuestionOptionsException("Unsupported question type: " + question.getQuestionType());
            }
            if (question.getMarks() == null || question.getMarks().compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidQuestionOptionsException("Every question must have marks greater than zero");
            }
            validateOptions(question.getOptions());
        });
    }

    private void validateOptions(List<QuestionOption> options) {
        if (options == null || options.size() < 4 || options.size() > 5) {
            throw new InvalidQuestionOptionsException("A question must contain between 4 and 5 options");
        }
        long correctAnswerCount = options.stream().filter(option -> Boolean.TRUE.equals(option.getCorrect())).count();
        if (correctAnswerCount != 1) {
            throw new InvalidQuestionOptionsException("Exactly one correct answer is required");
        }
    }

    private BigDecimal calculateTotalMarks(List<QuizQuestion> questions) {
        return questions.stream()
                .map(QuizQuestion::getMarks)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Quiz getQuizOrThrow(UUID quizId) {
        return quizRepository.findByIdAndIsActiveTrue(quizId)
                .orElseThrow(() -> new QuizNotFoundException(quizId));
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

    private void assertCanManageQuiz(Quiz quiz) {
        if (currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return;
        }

        if (currentUserService.hasAnyRole("TEACHER") && currentUserService.getCurrentUserId().equals(quiz.getTeacherId())) {
            return;
        }

        throw new UnauthorizedQuizAccessException("Teacher can only manage own quizzes");
    }

    private void assertDraft(Quiz quiz) {
        if (quiz.getStatus() != QuizStatus.DRAFT) {
            throw new InvalidQuestionOptionsException("Quiz can only be edited while it is in DRAFT status");
        }
    }

    private void validateQuizTimes(java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
            throw new InvalidQuestionOptionsException("Quiz start time must be before end time");
        }
    }

    private QuizCreationMethod resolveCreationMethod(CreateQuizRequest request) {
        return request.getCreationMethod() != null ? request.getCreationMethod() : QuizCreationMethod.MANUAL;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
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
