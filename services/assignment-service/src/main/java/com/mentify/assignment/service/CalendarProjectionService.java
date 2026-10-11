package com.mentify.assignment.service;

import com.mentify.assignment.entity.Assignment;
import com.mentify.quiz.entity.Quiz;

import java.util.UUID;

public interface CalendarProjectionService {
    void projectAssignmentIfEligible(Assignment assignment, String authorizationHeader);
    void cancelAssignment(Assignment assignment, String authorizationHeader);
    void projectQuizIfEligible(Quiz quiz, String authorizationHeader);
    void cancelQuiz(Quiz quiz, String authorizationHeader);
    void reconcileAssignment(UUID assignmentId, String authorizationHeader);
    void reconcileQuiz(UUID quizId, String authorizationHeader);
}
