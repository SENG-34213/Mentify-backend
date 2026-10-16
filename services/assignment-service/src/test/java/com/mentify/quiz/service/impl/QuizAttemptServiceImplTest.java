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
import com.mentify.quiz.enums.QuestionType;
import com.mentify.quiz.enums.QuizStatus;
import com.mentify.quiz.exception.OptionNotFoundException;
import com.mentify.quiz.exception.QuizAlreadySubmittedException;
import com.mentify.quiz.exception.QuizAttemptLimitExceededException;
import com.mentify.quiz.exception.QuizNotPublishedException;
import com.mentify.quiz.exception.StudentNotEnrolledException;
import com.mentify.quiz.exception.UnauthorizedQuizAccessException;
import com.mentify.quiz.repository.QuestionOptionRepository;
import com.mentify.quiz.repository.QuizAttemptRepository;
import com.mentify.quiz.repository.QuizQuestionRepository;
import com.mentify.quiz.repository.QuizRepository;
import com.mentify.quiz.repository.StudentAnswerRepository;
import com.mentify.quiz.security.CurrentUserService;
import com.mentify.quiz.service.QuizGradingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizAttemptServiceImplTest {

    private static final String AUTH_HEADER = "Bearer token";

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuizQuestionRepository quizQuestionRepository;

    @Mock
    private QuestionOptionRepository questionOptionRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private StudentAnswerRepository studentAnswerRepository;

    @Mock
    private QuizGradingService quizGradingService;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private EnrollmentServiceClient enrollmentServiceClient;

    @Mock
    private CurrentUserService currentUserService;

    private QuizAttemptServiceImpl quizAttemptService;

    @BeforeEach
    void setUp() {
        quizAttemptService = new QuizAttemptServiceImpl(
                quizRepository,
                quizQuestionRepository,
                questionOptionRepository,
                quizAttemptRepository,
                studentAnswerRepository,
                quizGradingService,
                userServiceClient,
                enrollmentServiceClient,
                currentUserService
        );
    }

    @Test
    void studentNotEnrolledInCourseCannotStartQuiz() {
        UUID studentId = UUID.randomUUID();
        Quiz quiz = publishedQuiz(1);

        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(userServiceClient.isStudentExists(studentId, AUTH_HEADER)).thenReturn(true);
        when(quizRepository.findByIdAndIsActiveTrue(quiz.getId())).thenReturn(Optional.of(quiz));
        when(enrollmentServiceClient.isStudentEnrolledInCourse(studentId, quiz.getCourseId(), AUTH_HEADER)).thenReturn(false);

        assertThatThrownBy(() -> quizAttemptService.startAttempt(quiz.getId(), AUTH_HEADER))
                .isInstanceOf(StudentNotEnrolledException.class);
    }

    @Test
    void studentCanStartPublishedQuiz() {
        UUID studentId = UUID.randomUUID();
        Quiz quiz = publishedQuiz(2);

        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(userServiceClient.isStudentExists(studentId, AUTH_HEADER)).thenReturn(true);
        when(quizRepository.findByIdAndIsActiveTrue(quiz.getId())).thenReturn(Optional.of(quiz));
        when(enrollmentServiceClient.isStudentEnrolledInCourse(studentId, quiz.getCourseId(), AUTH_HEADER)).thenReturn(true);
        when(quizAttemptRepository.countByQuizIdAndStudentIdAndIsActiveTrue(quiz.getId(), studentId)).thenReturn(0L);
        when(quizAttemptRepository.existsByQuizIdAndStudentIdAndStatusAndIsActiveTrue(
                quiz.getId(),
                studentId,
                AttemptStatus.IN_PROGRESS
        )).thenReturn(false);
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(invocation -> {
            QuizAttempt attempt = invocation.getArgument(0);
            attempt.setId(UUID.randomUUID());
            return attempt;
        });

        ApiResponse<StartQuizAttemptResponse> response = quizAttemptService.startAttempt(quiz.getId(), AUTH_HEADER);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getData().getStatus()).isEqualTo(AttemptStatus.IN_PROGRESS);
        assertThat(response.getData().getAttemptNumber()).isEqualTo(1);
    }

    @Test
    void studentCannotStartDraftQuiz() {
        UUID studentId = UUID.randomUUID();
        Quiz quiz = publishedQuiz(1);
        quiz.setStatus(QuizStatus.DRAFT);

        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(userServiceClient.isStudentExists(studentId, AUTH_HEADER)).thenReturn(true);
        when(quizRepository.findByIdAndIsActiveTrue(quiz.getId())).thenReturn(Optional.of(quiz));

        assertThatThrownBy(() -> quizAttemptService.startAttempt(quiz.getId(), AUTH_HEADER))
                .isInstanceOf(QuizNotPublishedException.class);
    }

    @Test
    void studentCannotExceedMaximumAttempts() {
        UUID studentId = UUID.randomUUID();
        Quiz quiz = publishedQuiz(1);

        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(userServiceClient.isStudentExists(studentId, AUTH_HEADER)).thenReturn(true);
        when(quizRepository.findByIdAndIsActiveTrue(quiz.getId())).thenReturn(Optional.of(quiz));
        when(enrollmentServiceClient.isStudentEnrolledInCourse(studentId, quiz.getCourseId(), AUTH_HEADER)).thenReturn(true);
        when(quizAttemptRepository.countByQuizIdAndStudentIdAndIsActiveTrue(quiz.getId(), studentId)).thenReturn(1L);

        assertThatThrownBy(() -> quizAttemptService.startAttempt(quiz.getId(), AUTH_HEADER))
                .isInstanceOf(QuizAttemptLimitExceededException.class);
    }

    @Test
    void studentCanSaveAnswer() {
        UUID studentId = UUID.randomUUID();
        QuizAttempt attempt = attempt(studentId, AttemptStatus.IN_PROGRESS);
        QuizQuestion question = question(attempt.getQuizId());
        QuestionOption option = option(question, true);

        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(quizAttemptRepository.findByIdAndStudentIdAndIsActiveTrue(attempt.getId(), studentId))
                .thenReturn(Optional.of(attempt));
        when(quizQuestionRepository.findByQuiz_IdAndIdAndIsActiveTrue(attempt.getQuizId(), question.getId()))
                .thenReturn(Optional.of(question));
        when(questionOptionRepository.findByIdAndQuestion_IdAndIsActiveTrue(option.getId(), question.getId()))
                .thenReturn(Optional.of(option));
        when(studentAnswerRepository.findByAttempt_IdAndQuestionIdAndIsActiveTrue(attempt.getId(), question.getId()))
                .thenReturn(Optional.empty());
        when(studentAnswerRepository.save(any(StudentAnswer.class))).thenAnswer(invocation -> {
            StudentAnswer answer = invocation.getArgument(0);
            answer.setId(UUID.randomUUID());
            return answer;
        });

        ApiResponse<StudentAnswerResponse> response = quizAttemptService.saveAnswer(
                attempt.getId(),
                SaveStudentAnswerRequest.builder()
                        .questionId(question.getId())
                        .selectedOptionId(option.getId())
                        .build()
        );

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getData().getSelectedOptionId()).isEqualTo(option.getId());
    }

    @Test
    void optionBelongingToAnotherQuestionIsRejected() {
        UUID studentId = UUID.randomUUID();
        QuizAttempt attempt = attempt(studentId, AttemptStatus.IN_PROGRESS);
        QuizQuestion question = question(attempt.getQuizId());
        UUID otherQuestionOptionId = UUID.randomUUID();

        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(quizAttemptRepository.findByIdAndStudentIdAndIsActiveTrue(attempt.getId(), studentId))
                .thenReturn(Optional.of(attempt));
        when(quizQuestionRepository.findByQuiz_IdAndIdAndIsActiveTrue(attempt.getQuizId(), question.getId()))
                .thenReturn(Optional.of(question));
        when(questionOptionRepository.findByIdAndQuestion_IdAndIsActiveTrue(otherQuestionOptionId, question.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizAttemptService.saveAnswer(
                attempt.getId(),
                SaveStudentAnswerRequest.builder()
                        .questionId(question.getId())
                        .selectedOptionId(otherQuestionOptionId)
                        .build()
        )).isInstanceOf(OptionNotFoundException.class);
    }

    @Test
    void studentCannotAccessAnotherStudentsAttempt() {
        UUID ownerStudentId = UUID.randomUUID();
        UUID authenticatedStudentId = UUID.randomUUID();
        QuizAttempt attempt = attempt(ownerStudentId, AttemptStatus.IN_PROGRESS);

        when(currentUserService.getCurrentUserId()).thenReturn(authenticatedStudentId);
        when(quizAttemptRepository.findWithAnswersByIdAndIsActiveTrue(attempt.getId())).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> quizAttemptService.submitAttempt(attempt.getId()))
                .isInstanceOf(UnauthorizedQuizAccessException.class);
    }

    @Test
    void submittedAttemptCannotBeSubmittedAgain() {
        UUID studentId = UUID.randomUUID();
        QuizAttempt attempt = attempt(studentId, AttemptStatus.SUBMITTED);

        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(quizAttemptRepository.findWithAnswersByIdAndIsActiveTrue(attempt.getId())).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> quizAttemptService.submitAttempt(attempt.getId()))
                .isInstanceOf(QuizAlreadySubmittedException.class);
    }

    @Test
    void submitAttemptCalculatesAndPersistsResult() {
        UUID studentId = UUID.randomUUID();
        Quiz quiz = publishedQuiz(1);
        QuizAttempt attempt = attempt(studentId, AttemptStatus.IN_PROGRESS);
        attempt.setQuizId(quiz.getId());
        QuizQuestion question = question(quiz.getId());
        StudentAnswer answer = StudentAnswer.builder()
                .attempt(attempt)
                .questionId(question.getId())
                .selectedOptionId(UUID.randomUUID())
                .answeredAt(LocalDateTime.now())
                .build();
        answer.setId(UUID.randomUUID());

        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(quizAttemptRepository.findWithAnswersByIdAndIsActiveTrue(attempt.getId())).thenReturn(Optional.of(attempt));
        when(quizRepository.findByIdAndIsActiveTrue(quiz.getId())).thenReturn(Optional.of(quiz));
        when(quizQuestionRepository.findByQuiz_IdAndIsActiveTrueOrderByQuestionOrderAsc(quiz.getId()))
                .thenReturn(List.of(question));
        when(studentAnswerRepository.findByAttempt_IdAndIsActiveTrue(attempt.getId())).thenReturn(List.of(answer));
        when(quizGradingService.grade(quiz, attempt, List.of(question), List.of(answer))).thenReturn(
                SubmitQuizResponse.builder()
                        .attemptId(attempt.getId())
                        .quizId(quiz.getId())
                        .score(new BigDecimal("2.00"))
                        .totalMarks(new BigDecimal("2.00"))
                        .percentage(new BigDecimal("100.00"))
                        .passed(true)
                        .build()
        );

        ApiResponse<SubmitQuizResponse> response = quizAttemptService.submitAttempt(attempt.getId());

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getData().getScore()).isEqualByComparingTo("2.00");
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(attempt.getSubmittedAt()).isNotNull();

        ArgumentCaptor<QuizAttempt> attemptCaptor = ArgumentCaptor.forClass(QuizAttempt.class);
        verify(quizAttemptRepository).save(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().getStatus()).isEqualTo(AttemptStatus.SUBMITTED);
    }

    private Quiz publishedQuiz(int maxAttempts) {
        Quiz quiz = Quiz.builder()
                .courseId(UUID.randomUUID())
                .teacherId(UUID.randomUUID())
                .title("Quiz")
                .durationMinutes(10)
                .totalMarks(new BigDecimal("2.00"))
                .passMark(new BigDecimal("1.00"))
                .maxAttempts(maxAttempts)
                .status(QuizStatus.PUBLISHED)
                .showResultImmediately(true)
                .build();
        quiz.setId(UUID.randomUUID());
        return quiz;
    }

    private QuizAttempt attempt(UUID studentId, AttemptStatus status) {
        QuizAttempt attempt = QuizAttempt.builder()
                .quizId(UUID.randomUUID())
                .studentId(studentId)
                .attemptNumber(1)
                .startedAt(LocalDateTime.now())
                .status(status)
                .score(BigDecimal.ZERO)
                .percentage(BigDecimal.ZERO)
                .build();
        attempt.setId(UUID.randomUUID());
        return attempt;
    }

    private QuizQuestion question(UUID quizId) {
        Quiz quiz = Quiz.builder()
                .courseId(UUID.randomUUID())
                .teacherId(UUID.randomUUID())
                .title("Quiz")
                .durationMinutes(10)
                .totalMarks(new BigDecimal("2.00"))
                .passMark(new BigDecimal("1.00"))
                .maxAttempts(1)
                .build();
        quiz.setId(quizId);

        QuizQuestion question = QuizQuestion.builder()
                .quiz(quiz)
                .questionText("Question")
                .questionType(QuestionType.MULTIPLE_CHOICE_SINGLE_ANSWER)
                .marks(new BigDecimal("2.00"))
                .questionOrder(1)
                .build();
        question.setId(UUID.randomUUID());
        return question;
    }

    private QuestionOption option(QuizQuestion question, boolean correct) {
        QuestionOption option = QuestionOption.builder()
                .question(question)
                .optionText("Option")
                .correct(correct)
                .optionOrder(1)
                .build();
        option.setId(UUID.randomUUID());
        return option;
    }
}
