package com.mentify.quiz.service.impl;

import com.mentify.quiz.dto.response.SubmitQuizResponse;
import com.mentify.quiz.entity.QuestionOption;
import com.mentify.quiz.entity.Quiz;
import com.mentify.quiz.entity.QuizAttempt;
import com.mentify.quiz.entity.QuizQuestion;
import com.mentify.quiz.entity.StudentAnswer;
import com.mentify.quiz.service.QuizGradingService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuizGradingServiceImpl implements QuizGradingService {

    @Override
    public SubmitQuizResponse grade(Quiz quiz, QuizAttempt attempt, List<QuizQuestion> questions, List<StudentAnswer> answers) {
        Map<UUID, StudentAnswer> answersByQuestionId = answers.stream()
                .collect(Collectors.toMap(StudentAnswer::getQuestionId, Function.identity()));

        BigDecimal score = BigDecimal.ZERO;

        for (QuizQuestion question : questions) {
            StudentAnswer answer = answersByQuestionId.get(question.getId());
            if (answer == null) {
                continue;
            }

            boolean correct = question.getOptions().stream()
                    .filter(option -> option.getId().equals(answer.getSelectedOptionId()))
                    .findFirst()
                    .map(QuestionOption::getCorrect)
                    .orElse(false);

            BigDecimal awardedMarks = correct ? question.getMarks() : BigDecimal.ZERO;
            answer.setCorrect(correct);
            answer.setAwardedMarks(awardedMarks);
            score = score.add(awardedMarks);
        }

        BigDecimal totalMarks = quiz.getTotalMarks();
        BigDecimal percentage = BigDecimal.ZERO;
        if (totalMarks != null && totalMarks.compareTo(BigDecimal.ZERO) > 0) {
            percentage = score.multiply(BigDecimal.valueOf(100))
                    .divide(totalMarks, 2, RoundingMode.HALF_UP);
        }

        boolean passed = quiz.getPassMark() != null && score.compareTo(quiz.getPassMark()) >= 0;
        attempt.setScore(score);
        attempt.setPercentage(percentage);

        return SubmitQuizResponse.builder()
                .attemptId(attempt.getId())
                .quizId(quiz.getId())
                .score(score)
                .totalMarks(totalMarks)
                .percentage(percentage)
                .passed(passed)
                .submittedAt(attempt.getSubmittedAt())
                .build();
    }
}
