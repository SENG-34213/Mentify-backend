package com.mentify.quiz.service.impl;

import com.mentify.quiz.dto.response.SubmitQuizResponse;
import com.mentify.quiz.entity.QuestionOption;
import com.mentify.quiz.entity.Quiz;
import com.mentify.quiz.entity.QuizAttempt;
import com.mentify.quiz.entity.QuizQuestion;
import com.mentify.quiz.entity.StudentAnswer;
import com.mentify.quiz.enums.QuestionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class QuizGradingServiceImplTest {

    private final QuizGradingServiceImpl gradingService = new QuizGradingServiceImpl();

    @Test
    void correctAnswerReceivesFullMarks() {
        QuizQuestion question = question(new BigDecimal("2.00"));
        QuestionOption correctOption = option(question, true);
        question.addOption(correctOption);
        question.addOption(option(question, false));

        StudentAnswer answer = answer(question.getId(), correctOption.getId());
        SubmitQuizResponse result = gradingService.grade(
                quiz(new BigDecimal("2.00"), new BigDecimal("1.00")),
                attempt(),
                List.of(question),
                List.of(answer)
        );

        assertThat(answer.getCorrect()).isTrue();
        assertThat(answer.getAwardedMarks()).isEqualByComparingTo("2.00");
        assertThat(result.getScore()).isEqualByComparingTo("2.00");
        assertThat(result.getPercentage()).isEqualByComparingTo("100.00");
        assertThat(result.getPassed()).isTrue();
    }

    @Test
    void wrongAnswerReceivesZeroMarks() {
        QuizQuestion question = question(new BigDecimal("2.00"));
        QuestionOption correctOption = option(question, true);
        QuestionOption wrongOption = option(question, false);
        question.addOption(correctOption);
        question.addOption(wrongOption);

        StudentAnswer answer = answer(question.getId(), wrongOption.getId());
        SubmitQuizResponse result = gradingService.grade(
                quiz(new BigDecimal("2.00"), new BigDecimal("1.00")),
                attempt(),
                List.of(question),
                List.of(answer)
        );

        assertThat(answer.getCorrect()).isFalse();
        assertThat(answer.getAwardedMarks()).isEqualByComparingTo("0.00");
        assertThat(result.getScore()).isEqualByComparingTo("0.00");
        assertThat(result.getPassed()).isFalse();
    }

    @Test
    void unansweredQuestionReceivesZeroMarksAndPercentageIsCalculatedCorrectly() {
        QuizQuestion answered = question(new BigDecimal("2.00"));
        QuestionOption correctOption = option(answered, true);
        answered.addOption(correctOption);
        answered.addOption(option(answered, false));

        QuizQuestion unanswered = question(new BigDecimal("3.00"));
        unanswered.addOption(option(unanswered, true));
        unanswered.addOption(option(unanswered, false));

        StudentAnswer answer = answer(answered.getId(), correctOption.getId());
        SubmitQuizResponse result = gradingService.grade(
                quiz(new BigDecimal("5.00"), new BigDecimal("2.00")),
                attempt(),
                List.of(answered, unanswered),
                List.of(answer)
        );

        assertThat(result.getScore()).isEqualByComparingTo("2.00");
        assertThat(result.getTotalMarks()).isEqualByComparingTo("5.00");
        assertThat(result.getPercentage()).isEqualByComparingTo("40.00");
        assertThat(result.getPassed()).isTrue();
    }

    private Quiz quiz(BigDecimal totalMarks, BigDecimal passMark) {
        Quiz quiz = Quiz.builder()
                .courseId(UUID.randomUUID())
                .teacherId(UUID.randomUUID())
                .title("Quiz")
                .durationMinutes(10)
                .totalMarks(totalMarks)
                .passMark(passMark)
                .maxAttempts(1)
                .showResultImmediately(true)
                .build();
        quiz.setId(UUID.randomUUID());
        return quiz;
    }

    private QuizAttempt attempt() {
        QuizAttempt attempt = QuizAttempt.builder()
                .quizId(UUID.randomUUID())
                .studentId(UUID.randomUUID())
                .attemptNumber(1)
                .startedAt(LocalDateTime.now())
                .submittedAt(LocalDateTime.now())
                .build();
        attempt.setId(UUID.randomUUID());
        return attempt;
    }

    private QuizQuestion question(BigDecimal marks) {
        QuizQuestion question = QuizQuestion.builder()
                .questionText("Question")
                .questionType(QuestionType.MULTIPLE_CHOICE_SINGLE_ANSWER)
                .marks(marks)
                .questionOrder(1)
                .build();
        question.setId(UUID.randomUUID());
        return question;
    }

    private QuestionOption option(QuizQuestion question, boolean correct) {
        QuestionOption option = QuestionOption.builder()
                .question(question)
                .optionText(correct ? "Correct" : UUID.randomUUID().toString())
                .correct(correct)
                .optionOrder(correct ? 1 : 2)
                .build();
        option.setId(UUID.randomUUID());
        return option;
    }

    private StudentAnswer answer(UUID questionId, UUID selectedOptionId) {
        StudentAnswer answer = StudentAnswer.builder()
                .questionId(questionId)
                .selectedOptionId(selectedOptionId)
                .answeredAt(LocalDateTime.now())
                .build();
        answer.setId(UUID.randomUUID());
        return answer;
    }
}
