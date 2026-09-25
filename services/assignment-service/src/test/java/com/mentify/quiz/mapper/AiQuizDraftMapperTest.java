package com.mentify.quiz.mapper;

import com.mentify.quiz.client.dto.AiGeneratedOptionResponse;
import com.mentify.quiz.client.dto.AiGeneratedQuestionResponse;
import com.mentify.quiz.client.dto.AiGeneratedQuizDraftResponse;
import com.mentify.quiz.dto.response.AiQuizDraftResponse;
import com.mentify.quiz.enums.QuestionType;
import com.mentify.quiz.enums.QuizGenerationDifficulty;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiQuizDraftMapperTest {

    private final AiQuizDraftMapper mapper = new AiQuizDraftMapper();

    @Test
    void mapsGeneratedQuestionsToManualQuestionRequestShape() {
        UUID courseId = UUID.randomUUID();
        AiGeneratedQuizDraftResponse generated = new AiGeneratedQuizDraftResponse();
        generated.setCourseId(courseId);
        generated.setDifficulty("MEDIUM");
        generated.setQuestionType("MULTIPLE_CHOICE_SINGLE_ANSWER");
        generated.setProvider("OPENAI");
        generated.setModel("gpt-4o");
        generated.setGeneratedAt(LocalDateTime.now());
        generated.setQuestions(List.of(question()));

        AiQuizDraftResponse draft = mapper.toDraftResponse(
                generated,
                QuizGenerationDifficulty.MEDIUM,
                QuestionType.MULTIPLE_CHOICE_SINGLE_ANSWER,
                new BigDecimal("1.00")
        );

        assertThat(draft.getCourseId()).isEqualTo(courseId);
        assertThat(draft.isSaved()).isFalse();
        assertThat(draft.isPublished()).isFalse();
        assertThat(draft.getQuestions()).hasSize(1);
        assertThat(draft.getQuestions().get(0).getQuestionType()).isEqualTo(QuestionType.MULTIPLE_CHOICE_SINGLE_ANSWER);
        assertThat(draft.getQuestions().get(0).getMarks()).isEqualByComparingTo("1.00");
        assertThat(draft.getQuestions().get(0).getOptions()).filteredOn(option -> Boolean.TRUE.equals(option.getCorrect()))
                .hasSize(1);
    }

    private AiGeneratedQuestionResponse question() {
        AiGeneratedQuestionResponse question = new AiGeneratedQuestionResponse();
        question.setQuestionText("Which OOP principle protects internal data?");
        question.setQuestionType("MULTIPLE_CHOICE_SINGLE_ANSWER");
        question.setQuestionOrder(1);
        question.setOptions(List.of(
                option("Encapsulation", true, 1),
                option("Inheritance", false, 2),
                option("Polymorphism", false, 3),
                option("Compilation", false, 4)
        ));
        return question;
    }

    private AiGeneratedOptionResponse option(String text, boolean correct, int order) {
        AiGeneratedOptionResponse option = new AiGeneratedOptionResponse();
        option.setOptionText(text);
        option.setCorrect(correct);
        option.setOptionOrder(order);
        return option;
    }
}
