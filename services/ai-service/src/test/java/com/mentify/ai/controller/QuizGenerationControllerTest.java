package com.mentify.ai.controller;

import com.mentify.ai.dto.request.QuizGenerationRequest;
import com.mentify.ai.dto.response.GeneratedQuizDraftResponse;
import com.mentify.ai.service.QuizGenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuizGenerationControllerTest {

    private final QuizGenerationService quizGenerationService = mock(QuizGenerationService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new QuizGenerationController(quizGenerationService))
            .build();

    @Test
    void bindsMultipartFileAndRequestParameters() throws Exception {
        UUID courseId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "lesson.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "pdf".getBytes()
        );
        when(quizGenerationService.generateQuiz(any(), eq(file)))
                .thenReturn(GeneratedQuizDraftResponse.builder()
                        .courseId(courseId)
                        .questionCount(3)
                        .difficulty("MEDIUM")
                        .questionType("MULTIPLE_CHOICE_SINGLE_ANSWER")
                        .build());

        mockMvc.perform(multipart("/api/ai/quizzes/generate")
                        .file(file)
                        .param("courseId", courseId.toString())
                        .param("questionCount", "3")
                        .param("difficulty", "MEDIUM")
                        .param("questionType", "MULTIPLE_CHOICE_SINGLE_ANSWER")
                        .param("userPrompt", "Focus on architecture tradeoffs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.courseId").value(courseId.toString()));

        verify(quizGenerationService).generateQuiz(
                org.mockito.ArgumentMatchers.argThat((QuizGenerationRequest request) ->
                        courseId.equals(request.getCourseId())
                                && request.getQuestionCount().equals(3)
                                && request.getDifficulty().equals("MEDIUM")
                                && request.getQuestionType().equals("MULTIPLE_CHOICE_SINGLE_ANSWER")
                                && request.getUserPrompt().equals("Focus on architecture tradeoffs")),
                eq(file)
        );
    }
}
