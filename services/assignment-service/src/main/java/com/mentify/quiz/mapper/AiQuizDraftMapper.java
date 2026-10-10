package com.mentify.quiz.mapper;

import com.mentify.quiz.client.dto.AiGeneratedOptionResponse;
import com.mentify.quiz.client.dto.AiGeneratedQuestionResponse;
import com.mentify.quiz.client.dto.AiGeneratedQuizDraftResponse;
import com.mentify.quiz.dto.request.CreateQuestionRequest;
import com.mentify.quiz.dto.request.QuestionOptionRequest;
import com.mentify.quiz.dto.response.AiQuizDraftResponse;
import com.mentify.quiz.enums.QuestionType;
import com.mentify.quiz.enums.QuizCreationMethod;
import com.mentify.quiz.enums.QuizGenerationDifficulty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
public class AiQuizDraftMapper {

    public AiQuizDraftResponse toDraftResponse(
            AiGeneratedQuizDraftResponse response,
            QuizGenerationDifficulty difficulty,
            QuestionType questionType,
            BigDecimal defaultQuestionMarks
    ) {
        List<CreateQuestionRequest> questions = response.getQuestions().stream()
                .sorted(Comparator.comparing(AiGeneratedQuestionResponse::getQuestionOrder))
                .map(question -> toCreateQuestionRequest(question, questionType, defaultQuestionMarks))
                .toList();

        return AiQuizDraftResponse.builder()
                .courseId(response.getCourseId())
                .questionCount(questions.size())
                .difficulty(difficulty)
                .questionType(questionType)
                .creationMethod(QuizCreationMethod.AI_GENERATED)
                .questions(questions)
                .provider(response.getProvider())
                .model(response.getModel())
                .generatedAt(response.getGeneratedAt())
                .saved(false)
                .published(false)
                .build();
    }

    private CreateQuestionRequest toCreateQuestionRequest(
            AiGeneratedQuestionResponse question,
            QuestionType questionType,
            BigDecimal defaultQuestionMarks
    ) {
        return CreateQuestionRequest.builder()
                .questionText(question.getQuestionText())
                .questionType(questionType)
                .marks(defaultQuestionMarks)
                .questionOrder(question.getQuestionOrder())
                .options(question.getOptions().stream()
                        .sorted(Comparator.comparing(AiGeneratedOptionResponse::getOptionOrder))
                        .map(this::toQuestionOptionRequest)
                        .toList())
                .build();
    }

    private QuestionOptionRequest toQuestionOptionRequest(AiGeneratedOptionResponse option) {
        return QuestionOptionRequest.builder()
                .optionText(option.getOptionText())
                .correct(Boolean.TRUE.equals(option.getCorrect()))
                .optionOrder(option.getOptionOrder())
                .build();
    }
}
