package com.mentify.quiz.mapper;

import com.mentify.quiz.dto.response.QuizResponse;
import com.mentify.quiz.dto.response.StudentOptionResponse;
import com.mentify.quiz.dto.response.StudentQuestionResponse;
import com.mentify.quiz.dto.response.StudentQuizResponse;
import com.mentify.quiz.dto.response.TeacherOptionResponse;
import com.mentify.quiz.dto.response.TeacherQuestionResponse;
import com.mentify.quiz.entity.QuestionOption;
import com.mentify.quiz.entity.Quiz;
import com.mentify.quiz.entity.QuizQuestion;

import java.util.Comparator;
import java.util.List;

public final class QuizMapper {

    private QuizMapper() {
    }

    public static QuizResponse toTeacherQuizResponse(Quiz quiz, List<QuizQuestion> questions) {
        return QuizResponse.builder()
                .id(quiz.getId())
                .courseId(quiz.getCourseId())
                .teacherId(quiz.getTeacherId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .durationMinutes(quiz.getDurationMinutes())
                .totalMarks(quiz.getTotalMarks())
                .passMark(quiz.getPassMark())
                .startTime(quiz.getStartTime())
                .endTime(quiz.getEndTime())
                .maxAttempts(quiz.getMaxAttempts())
                .status(quiz.getStatus())
                .showResultImmediately(quiz.getShowResultImmediately())
                .questions(questions == null ? null : questions.stream()
                        .sorted(Comparator.comparing(QuizQuestion::getQuestionOrder))
                        .map(QuizMapper::toTeacherQuestionResponse)
                        .toList())
                .createdAt(quiz.getCreatedAt())
                .updatedAt(quiz.getUpdatedAt())
                .build();
    }

    public static TeacherQuestionResponse toTeacherQuestionResponse(QuizQuestion question) {
        return TeacherQuestionResponse.builder()
                .id(question.getId())
                .questionText(question.getQuestionText())
                .questionType(question.getQuestionType())
                .marks(question.getMarks())
                .questionOrder(question.getQuestionOrder())
                .options(question.getOptions().stream()
                        .sorted(Comparator.comparing(QuestionOption::getOptionOrder))
                        .map(QuizMapper::toTeacherOptionResponse)
                        .toList())
                .build();
    }

    public static TeacherOptionResponse toTeacherOptionResponse(QuestionOption option) {
        return TeacherOptionResponse.builder()
                .id(option.getId())
                .optionText(option.getOptionText())
                .correct(option.getCorrect())
                .optionOrder(option.getOptionOrder())
                .build();
    }

    public static StudentQuizResponse toStudentQuizResponse(Quiz quiz, List<QuizQuestion> questions) {
        return StudentQuizResponse.builder()
                .id(quiz.getId())
                .courseId(quiz.getCourseId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .durationMinutes(quiz.getDurationMinutes())
                .totalMarks(quiz.getTotalMarks())
                .passMark(quiz.getPassMark())
                .startTime(quiz.getStartTime())
                .endTime(quiz.getEndTime())
                .maxAttempts(quiz.getMaxAttempts())
                .questions(questions == null ? null : questions.stream()
                        .sorted(Comparator.comparing(QuizQuestion::getQuestionOrder))
                        .map(QuizMapper::toStudentQuestionResponse)
                        .toList())
                .build();
    }

    public static StudentQuestionResponse toStudentQuestionResponse(QuizQuestion question) {
        return StudentQuestionResponse.builder()
                .id(question.getId())
                .questionText(question.getQuestionText())
                .questionType(question.getQuestionType())
                .marks(question.getMarks())
                .questionOrder(question.getQuestionOrder())
                .options(question.getOptions().stream()
                        .sorted(Comparator.comparing(QuestionOption::getOptionOrder))
                        .map(QuizMapper::toStudentOptionResponse)
                        .toList())
                .build();
    }

    public static StudentOptionResponse toStudentOptionResponse(QuestionOption option) {
        return StudentOptionResponse.builder()
                .id(option.getId())
                .optionText(option.getOptionText())
                .optionOrder(option.getOptionOrder())
                .build();
    }
}
