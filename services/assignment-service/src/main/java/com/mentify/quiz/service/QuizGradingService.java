package com.mentify.quiz.service;

import com.mentify.quiz.dto.response.SubmitQuizResponse;
import com.mentify.quiz.entity.Quiz;
import com.mentify.quiz.entity.QuizAttempt;
import com.mentify.quiz.entity.QuizQuestion;
import com.mentify.quiz.entity.StudentAnswer;

import java.util.List;

public interface QuizGradingService {

    SubmitQuizResponse grade(Quiz quiz, QuizAttempt attempt, List<QuizQuestion> questions, List<StudentAnswer> answers);
}
