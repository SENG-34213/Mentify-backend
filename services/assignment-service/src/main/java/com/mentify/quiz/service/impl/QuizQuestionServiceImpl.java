package com.mentify.quiz.service.impl;

import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.dto.request.CreateQuestionRequest;
import com.mentify.quiz.dto.request.QuestionOptionRequest;
import com.mentify.quiz.dto.response.TeacherQuestionResponse;
import com.mentify.quiz.entity.QuestionOption;
import com.mentify.quiz.entity.Quiz;
import com.mentify.quiz.entity.QuizQuestion;
import com.mentify.quiz.enums.QuestionType;
import com.mentify.quiz.enums.QuizStatus;
import com.mentify.quiz.exception.InvalidQuestionOptionsException;
import com.mentify.quiz.exception.QuestionNotFoundException;
import com.mentify.quiz.exception.QuizNotFoundException;
import com.mentify.quiz.exception.UnauthorizedQuizAccessException;
import com.mentify.quiz.mapper.QuizMapper;
import com.mentify.quiz.repository.QuizQuestionRepository;
import com.mentify.quiz.repository.QuizRepository;
import com.mentify.quiz.security.CurrentUserService;
import com.mentify.quiz.service.QuizQuestionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuizQuestionServiceImpl implements QuizQuestionService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public ApiResponse<TeacherQuestionResponse> createQuestion(
            UUID quizId,
            CreateQuestionRequest request,
            String authorizationHeader
    ) {
        Quiz quiz = quizRepository.findByIdAndIsActiveTrue(quizId)
                .orElseThrow(() -> new QuizNotFoundException(quizId));

        assertCanManageQuiz(quiz);
        assertDraft(quiz);
        validateQuestionRequest(request);

        QuizQuestion question = toQuestion(request);
        quiz.addQuestion(question);
        QuizQuestion savedQuestion = quizQuestionRepository.save(question);
        refreshTotalMarks(quiz);

        return response(HttpStatus.CREATED, "Question created successfully",
                QuizMapper.toTeacherQuestionResponse(savedQuestion));
    }

    @Override
    @Transactional
    public ApiResponse<TeacherQuestionResponse> updateQuestion(
            UUID quizId,
            UUID questionId,
            CreateQuestionRequest request,
            String authorizationHeader
    ) {
        Quiz quiz = quizRepository.findByIdAndIsActiveTrue(quizId)
                .orElseThrow(() -> new QuizNotFoundException(quizId));
        QuizQuestion question = quizQuestionRepository.findByQuiz_IdAndIdAndIsActiveTrue(quizId, questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        assertCanManageQuiz(quiz);
        assertDraft(quiz);
        validateQuestionRequest(request);

        question.setQuestionText(request.getQuestionText().trim());
        question.setQuestionType(request.getQuestionType());
        question.setMarks(request.getMarks());
        question.setQuestionOrder(request.getQuestionOrder());
        question.getOptions().clear();
        request.getOptions().forEach(optionRequest -> question.addOption(toOption(optionRequest)));

        QuizQuestion savedQuestion = quizQuestionRepository.save(question);
        refreshTotalMarks(quiz);

        return response(HttpStatus.OK, "Question updated successfully",
                QuizMapper.toTeacherQuestionResponse(savedQuestion));
    }

    @Override
    @Transactional
    public ApiResponse<Object> deleteQuestion(UUID quizId, UUID questionId, String authorizationHeader) {
        Quiz quiz = quizRepository.findByIdAndIsActiveTrue(quizId)
                .orElseThrow(() -> new QuizNotFoundException(quizId));
        QuizQuestion question = quizQuestionRepository.findByQuiz_IdAndIdAndIsActiveTrue(quizId, questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        assertCanManageQuiz(quiz);
        assertDraft(quiz);

        quiz.getQuestions().remove(question);
        quizQuestionRepository.delete(question);
        refreshTotalMarks(quiz);

        return response(HttpStatus.OK, "Question deleted successfully", null);
    }

    private QuizQuestion toQuestion(CreateQuestionRequest request) {
        QuizQuestion question = QuizQuestion.builder()
                .questionText(request.getQuestionText().trim())
                .questionType(request.getQuestionType())
                .marks(request.getMarks())
                .questionOrder(request.getQuestionOrder())
                .build();
        request.getOptions().forEach(optionRequest -> question.addOption(toOption(optionRequest)));
        return question;
    }

    private QuestionOption toOption(QuestionOptionRequest request) {
        return QuestionOption.builder()
                .optionText(request.getOptionText().trim())
                .correct(Boolean.TRUE.equals(request.getCorrect()))
                .optionOrder(request.getOptionOrder())
                .build();
    }

    public void validateQuestionRequest(CreateQuestionRequest request) {
        if (request.getQuestionType() != QuestionType.MULTIPLE_CHOICE_SINGLE_ANSWER) {
            throw new InvalidQuestionOptionsException("Only MULTIPLE_CHOICE_SINGLE_ANSWER is supported");
        }

        if (request.getMarks() == null || request.getMarks().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidQuestionOptionsException("Question marks must be greater than zero");
        }

        if (request.getOptions() == null || request.getOptions().size() < 4 || request.getOptions().size() > 5) {
            throw new InvalidQuestionOptionsException("A question must contain between 4 and 5 options");
        }

        long correctAnswerCount = request.getOptions().stream()
                .filter(option -> Boolean.TRUE.equals(option.getCorrect()))
                .count();
        if (correctAnswerCount != 1) {
            throw new InvalidQuestionOptionsException("Exactly one correct answer is required");
        }

        Set<String> optionTexts = new HashSet<>();
        for (QuestionOptionRequest option : request.getOptions()) {
            String normalizedText = option.getOptionText() == null
                    ? ""
                    : option.getOptionText().trim().toLowerCase(Locale.ROOT);
            if (normalizedText.isBlank()) {
                throw new InvalidQuestionOptionsException("Option text is required");
            }
            if (!optionTexts.add(normalizedText)) {
                throw new InvalidQuestionOptionsException("Duplicate option text is not allowed within the same question");
            }
        }
    }

    private void refreshTotalMarks(Quiz quiz) {
        BigDecimal totalMarks = quizQuestionRepository.findByQuiz_IdAndIsActiveTrueOrderByQuestionOrderAsc(quiz.getId())
                .stream()
                .map(QuizQuestion::getMarks)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        quiz.setTotalMarks(totalMarks);
        quizRepository.save(quiz);
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
            throw new InvalidQuestionOptionsException("Questions can only be edited while the quiz is in DRAFT status");
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
