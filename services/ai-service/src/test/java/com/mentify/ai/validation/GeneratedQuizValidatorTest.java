package com.mentify.ai.validation;

import com.mentify.ai.dto.response.GeneratedOptionResponse;
import com.mentify.ai.dto.response.GeneratedQuestionResponse;
import com.mentify.ai.exception.AiInvalidGenerationException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeneratedQuizValidatorTest {

    private final GeneratedQuizValidator validator = new GeneratedQuizValidator();

    @Test
    void rejectsDuplicateQuestions() {
        List<GeneratedQuestionResponse> questions = List.of(validQuestion("What is encapsulation?", 1),
                validQuestion(" What is encapsulation? ", 2));

        assertThatThrownBy(() -> validator.validate(questions, 2, "MULTIPLE_CHOICE_SINGLE_ANSWER"))
                .isInstanceOf(AiInvalidGenerationException.class)
                .hasMessageContaining("duplicate questions");
    }

    @Test
    void rejectsMultipleCorrectOptions() {
        GeneratedQuestionResponse question = validQuestion("What is inheritance?", 1);
        question.getOptions().get(1).setCorrect(true);

        assertThatThrownBy(() -> validator.validate(List.of(question), 1, "MULTIPLE_CHOICE_SINGLE_ANSWER"))
                .isInstanceOf(AiInvalidGenerationException.class)
                .hasMessageContaining("exactly one correct");
    }

    @Test
    void rejectsDuplicateOptions() {
        GeneratedQuestionResponse question = validQuestion("What is polymorphism?", 1);
        question.getOptions().get(2).setOptionText("Encapsulation");

        assertThatThrownBy(() -> validator.validate(List.of(question), 1, "MULTIPLE_CHOICE_SINGLE_ANSWER"))
                .isInstanceOf(AiInvalidGenerationException.class)
                .hasMessageContaining("duplicate option");
    }

    private GeneratedQuestionResponse validQuestion(String text, int order) {
        return GeneratedQuestionResponse.builder()
                .questionText(text)
                .questionType("MULTIPLE_CHOICE_SINGLE_ANSWER")
                .questionOrder(order)
                .options(new ArrayList<>(List.of(
                        GeneratedOptionResponse.builder().optionText("Encapsulation").correct(true).optionOrder(1).build(),
                        GeneratedOptionResponse.builder().optionText("Inheritance").correct(false).optionOrder(2).build(),
                        GeneratedOptionResponse.builder().optionText("Polymorphism").correct(false).optionOrder(3).build(),
                        GeneratedOptionResponse.builder().optionText("Compilation").correct(false).optionOrder(4).build()
                )))
                .build();
    }
}
