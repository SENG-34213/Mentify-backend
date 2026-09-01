package com.mentify.quiz.service.impl;

import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.client.CourseServiceClient;
import com.mentify.quiz.client.EnrollmentServiceClient;
import com.mentify.quiz.client.dto.CourseLookupResponse;
import com.mentify.quiz.dto.request.CreateQuizRequest;
import com.mentify.quiz.dto.response.QuizResponse;
import com.mentify.quiz.dto.response.StudentQuestionResponse;
import com.mentify.quiz.dto.response.StudentQuizResponse;
import com.mentify.quiz.entity.QuestionOption;
import com.mentify.quiz.entity.Quiz;
import com.mentify.quiz.entity.QuizQuestion;
import com.mentify.quiz.enums.QuestionType;
import com.mentify.quiz.enums.QuizStatus;
import com.mentify.quiz.mapper.QuizMapper;
import com.mentify.quiz.repository.QuizQuestionRepository;
import com.mentify.quiz.repository.QuizRepository;
import com.mentify.quiz.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizServiceImplTest {

    private static final String AUTH_HEADER = "Bearer token";

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuizQuestionRepository quizQuestionRepository;

    @Mock
    private CourseServiceClient courseServiceClient;

    @Mock
    private EnrollmentServiceClient enrollmentServiceClient;

    @Mock
    private CurrentUserService currentUserService;

    private QuizServiceImpl quizService;

    @BeforeEach
    void setUp() {
        quizService = new QuizServiceImpl(
                quizRepository,
                quizQuestionRepository,
                courseServiceClient,
                enrollmentServiceClient,
                currentUserService
        );
    }

    @Test
    void teacherCanCreateQuizForAssignedCourse() {
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        CreateQuizRequest request = CreateQuizRequest.builder()
                .courseId(courseId)
                .title(" Java Basics Quiz ")
                .description(" Basic Java knowledge assessment ")
                .durationMinutes(15)
                .passMark(new BigDecimal("10.00"))
                .maxAttempts(1)
                .showResultImmediately(true)
                .build();

        when(courseServiceClient.getCourseById(courseId, AUTH_HEADER)).thenReturn(courseResponse(courseId, teacherId));
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(teacherId);
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> {
            Quiz quiz = invocation.getArgument(0);
            quiz.setId(UUID.randomUUID());
            return quiz;
        });

        ApiResponse<QuizResponse> response = quizService.createQuiz(request, AUTH_HEADER);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getData().getStatus()).isEqualTo(QuizStatus.DRAFT);
        assertThat(response.getData().getTitle()).isEqualTo("Java Basics Quiz");
        assertThat(response.getData().getTeacherId()).isEqualTo(teacherId);
        assertThat(response.getData().getTotalMarks()).isEqualByComparingTo("0.00");

        ArgumentCaptor<Quiz> quizCaptor = ArgumentCaptor.forClass(Quiz.class);
        verify(quizRepository).save(quizCaptor.capture());
        assertThat(quizCaptor.getValue().getCourseId()).isEqualTo(courseId);
        assertThat(quizCaptor.getValue().getTeacherId()).isEqualTo(teacherId);
    }

    @Test
    void studentQuizResponseNeverContainsCorrectAnswerFlag() {
        Quiz quiz = Quiz.builder()
                .courseId(UUID.randomUUID())
                .teacherId(UUID.randomUUID())
                .title("Quiz")
                .durationMinutes(10)
                .totalMarks(new BigDecimal("2.00"))
                .passMark(new BigDecimal("1.00"))
                .maxAttempts(1)
                .status(QuizStatus.PUBLISHED)
                .showResultImmediately(true)
                .build();
        quiz.setId(UUID.randomUUID());

        QuizQuestion question = QuizQuestion.builder()
                .quiz(quiz)
                .questionText("Question")
                .questionType(QuestionType.MULTIPLE_CHOICE_SINGLE_ANSWER)
                .marks(new BigDecimal("2.00"))
                .questionOrder(1)
                .build();
        question.setId(UUID.randomUUID());

        QuestionOption option = QuestionOption.builder()
                .question(question)
                .optionText("Correct option")
                .correct(true)
                .optionOrder(1)
                .build();
        option.setId(UUID.randomUUID());
        question.addOption(option);

        StudentQuizResponse response = QuizMapper.toStudentQuizResponse(quiz, List.of(question));
        StudentQuestionResponse studentQuestion = response.getQuestions().get(0);

        assertThat(studentQuestion.getOptions().get(0).getOptionText()).isEqualTo("Correct option");
        assertThat(studentQuestion.getOptions().get(0).getClass().getDeclaredFields())
                .extracting("name")
                .doesNotContain("correct", "isCorrect", "correctAnswer");
    }

    private ApiResponse<CourseLookupResponse> courseResponse(UUID courseId, UUID teacherId) {
        CourseLookupResponse course = new CourseLookupResponse();
        course.setId(courseId);
        course.setAssignedTeacherId(teacherId);
        course.setPublished(true);
        course.setVisible(true);

        return ApiResponse.<CourseLookupResponse>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .data(course)
                .build();
    }
}
