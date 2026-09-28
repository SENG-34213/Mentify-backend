package com.mentify.ai.validation;

import com.mentify.ai.dto.response.GeneratedOptionResponse;
import com.mentify.ai.dto.response.GeneratedQuestionResponse;
import com.mentify.ai.exception.AiInvalidGenerationException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class GeneratedQuizValidator {

    public void validate(List<GeneratedQuestionResponse> questions, int expectedCount, String expectedQuestionType) {
        if (questions == null || questions.isEmpty()) {
            throw new AiInvalidGenerationException("AI response did not contain any questions");
        }
        if (questions.size() > expectedCount) {
            throw new AiInvalidGenerationException("AI response contained more questions than requested");
        }

        Set<String> questionTexts = new HashSet<>();
        for (int i = 0; i < questions.size(); i++) {
            GeneratedQuestionResponse question = questions.get(i);
            validateQuestion(question, i + 1, expectedQuestionType);
            String normalizedQuestion = normalize(question.getQuestionText());
            if (!questionTexts.add(normalizedQuestion)) {
                throw new AiInvalidGenerationException("AI response contained duplicate questions");
            }
        }
    }

    private void validateQuestion(GeneratedQuestionResponse question, int expectedOrder, String expectedQuestionType) {
        if (question == null || isBlank(question.getQuestionText())) {
            throw new AiInvalidGenerationException("AI response contained a blank question");
        }
        if (!expectedQuestionType.equals(question.getQuestionType())) {
            throw new AiInvalidGenerationException("AI response contained an unsupported question type");
        }
        if (question.getQuestionOrder() == null) {
            question.setQuestionOrder(expectedOrder);
        }
        validateOptions(question.getOptions());
    }

    private void validateOptions(List<GeneratedOptionResponse> options) {
        if (options == null || options.size() != 4) {
            throw new AiInvalidGenerationException("AI response questions must contain exactly 4 options");
        }

        long correctCount = options.stream().filter(option -> Boolean.TRUE.equals(option.getCorrect())).count();
        if (correctCount != 1) {
            throw new AiInvalidGenerationException("AI response questions must contain exactly one correct option");
        }

        Set<String> optionTexts = new HashSet<>();
        for (int i = 0; i < options.size(); i++) {
            GeneratedOptionResponse option = options.get(i);
            if (option == null || isBlank(option.getOptionText())) {
                throw new AiInvalidGenerationException("AI response contained a blank option");
            }
            if (!optionTexts.add(normalize(option.getOptionText()))) {
                throw new AiInvalidGenerationException("AI response contained duplicate option text");
            }
            if (option.getOptionOrder() == null) {
                option.setOptionOrder(i + 1);
            }
            option.setCorrect(Boolean.TRUE.equals(option.getCorrect()));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
