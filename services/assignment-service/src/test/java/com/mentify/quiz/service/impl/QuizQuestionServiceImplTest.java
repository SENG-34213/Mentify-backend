package com.mentify.quiz.service.impl;

import com.mentify.quiz.dto.request.CreateQuestionRequest;
import com.mentify.quiz.dto.request.QuestionOptionRequest;
import com.mentify.quiz.enums.QuestionType;
import com.mentify.quiz.exception.InvalidQuestionOptionsException;
import com.mentify.quiz.repository.QuizQuestionRepository;
import com.mentify.quiz.repository.QuizRepository;
import com.mentify.quiz.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class QuizQuestionServiceImplTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuizQuestionRepository quizQuestionRepository;

    @Mock
    private CurrentUserService currentUserService;

    private QuizQuestionServiceImpl quizQuestionService;

    @BeforeEach
    void setUp() {
        quizQuestionService = new QuizQuestionServiceImpl(
                quizRepository,
                quizQuestionRepository,
                currentUserService
        );
    }

    @Test
    void questionWithThreeOptionsIsRejected() {
        CreateQuestionRequest request = validQuestion();
        request.setOptions(request.getOptions().subList(0, 3));

        assertThatThrownBy(() -> quizQuestionService.validateQuestionRequest(request))
                .isInstanceOf(InvalidQuestionOptionsException.class)
                .hasMessage("A question must contain between 4 and 5 options");
    }

    @Test
    void questionWithSixOptionsIsRejected() {
        CreateQuestionRequest request = validQuestion();
        List<QuestionOptionRequest> options = new ArrayList<>(request.getOptions());
        options.add(QuestionOptionRequest.builder()
                .optionText("sealed")
                .correct(false)
                .optionOrder(5)
                .build());
        options.add(QuestionOptionRequest.builder()
                .optionText("permits")
                .correct(false)
                .optionOrder(6)
                .build());
        request.setOptions(options);

        assertThatThrownBy(() -> quizQuestionService.validateQuestionRequest(request))
                .isInstanceOf(InvalidQuestionOptionsException.class)
                .hasMessage("A question must contain between 4 and 5 options");
    }

    @Test
    void questionWithZeroCorrectAnswersIsRejected() {
        CreateQuestionRequest request = validQuestion();
        request.getOptions().forEach(option -> option.setCorrect(false));

        assertThatThrownBy(() -> quizQuestionService.validateQuestionRequest(request))
                .isInstanceOf(InvalidQuestionOptionsException.class)
                .hasMessage("Exactly one correct answer is required");
    }

    @Test
    void questionWithMoreThanOneCorrectAnswerIsRejected() {
        CreateQuestionRequest request = validQuestion();
        request.getOptions().get(0).setCorrect(true);
        request.getOptions().get(1).setCorrect(true);

        assertThatThrownBy(() -> quizQuestionService.validateQuestionRequest(request))
                .isInstanceOf(InvalidQuestionOptionsException.class)
                .hasMessage("Exactly one correct answer is required");
    }

    @Test
    void duplicateOptionTextIsRejected() {
        CreateQuestionRequest request = validQuestion();
        request.getOptions().get(2).setOptionText(" Extends ");

        assertThatThrownBy(() -> quizQuestionService.validateQuestionRequest(request))
                .isInstanceOf(InvalidQuestionOptionsException.class)
                .hasMessage("Duplicate option text is not allowed within the same question");
    }

    private CreateQuestionRequest validQuestion() {
        return CreateQuestionRequest.builder()
                .questionText("Which keyword is used to inherit a class in Java?")
                .questionType(QuestionType.MULTIPLE_CHOICE_SINGLE_ANSWER)
                .marks(new BigDecimal("2.00"))
                .questionOrder(1)
                .options(new ArrayList<>(List.of(
                        QuestionOptionRequest.builder().optionText("implement").correct(false).optionOrder(1).build(),
                        QuestionOptionRequest.builder().optionText("extends").correct(true).optionOrder(2).build(),
                        QuestionOptionRequest.builder().optionText("inherit").correct(false).optionOrder(3).build(),
                        QuestionOptionRequest.builder().optionText("super").correct(false).optionOrder(4).build()
                )))
                .build();
    }
}
