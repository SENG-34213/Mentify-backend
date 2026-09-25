package com.mentify.quiz.service.impl;

import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.client.AiQuizGenerationClient;
import com.mentify.quiz.client.dto.AiGeneratedOptionResponse;
import com.mentify.quiz.client.dto.AiGeneratedQuestionResponse;
import com.mentify.quiz.client.dto.AiGeneratedQuizDraftResponse;
import com.mentify.quiz.config.AiQuizGenerationProperties;
import com.mentify.quiz.dto.response.AiQuizDraftResponse;
import com.mentify.quiz.enums.QuestionType;
import com.mentify.quiz.enums.QuizGenerationDifficulty;
import com.mentify.quiz.exception.AiQuizGenerationException;
import com.mentify.quiz.exception.InvalidQuestionOptionsException;
import com.mentify.quiz.exception.UnauthorizedQuizAccessException;
import com.mentify.quiz.mapper.AiQuizDraftMapper;
import com.mentify.quiz.service.CourseQuizAuthorizationService;
import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiQuizGenerationServiceImplTest {

    private static final String AUTH_HEADER = "Bearer token";

    @Mock
    private CourseQuizAuthorizationService courseQuizAuthorizationService;

    @Mock
    private AiQuizGenerationClient aiQuizGenerationClient;

    private AiQuizGenerationServiceImpl service;

    @BeforeEach
    void setUp() {
        AiQuizGenerationProperties properties = new AiQuizGenerationProperties();
        properties.setMaxQuestionCount(20);
        properties.setDefaultQuestionMarks(new BigDecimal("1.00"));
        service = new AiQuizGenerationServiceImpl(
                courseQuizAuthorizationService,
                aiQuizGenerationClient,
                new AiQuizDraftMapper(),
                properties
        );
    }

    @Test
    void returnsUnsavedDraftAfterCourseAuthorization() {
        UUID courseId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "oop.pdf", "application/pdf", "pdf".getBytes());
        when(aiQuizGenerationClient.generateQuiz(
                eq(file),
                eq(courseId),
                eq(1),
                eq("MEDIUM"),
                eq("MULTIPLE_CHOICE_SINGLE_ANSWER"),
                eq(AUTH_HEADER)
        )).thenReturn(ApiResponse.<AiGeneratedQuizDraftResponse>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .data(generatedDraft(courseId))
                .build());

        ApiResponse<AiQuizDraftResponse> response = service.generateDraft(
                file,
                courseId,
                1,
                QuizGenerationDifficulty.MEDIUM,
                QuestionType.MULTIPLE_CHOICE_SINGLE_ANSWER,
                AUTH_HEADER
        );

        verify(courseQuizAuthorizationService).assertCanCreateQuizForCourse(courseId, AUTH_HEADER);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getData().isSaved()).isFalse();
        assertThat(response.getData().isPublished()).isFalse();
        assertThat(response.getData().getQuestions()).hasSize(1);
        assertThat(response.getData().getQuestions().get(0).getMarks()).isEqualByComparingTo("1.00");
    }

    @Test
    void publicTestingRequestSkipsCourseAuthorizationWhenAuthorizationHeaderIsMissing() {
        UUID courseId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "oop.pdf", "application/pdf", "pdf".getBytes());
        when(aiQuizGenerationClient.generateQuiz(
                eq(file),
                eq(courseId),
                eq(1),
                eq("MEDIUM"),
                eq("MULTIPLE_CHOICE_SINGLE_ANSWER"),
                eq(null)
        )).thenReturn(ApiResponse.<AiGeneratedQuizDraftResponse>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .data(generatedDraft(courseId))
                .build());

        ApiResponse<AiQuizDraftResponse> response = service.generateDraft(
                file,
                courseId,
                1,
                QuizGenerationDifficulty.MEDIUM,
                QuestionType.MULTIPLE_CHOICE_SINGLE_ANSWER,
                null
        );

        verify(courseQuizAuthorizationService, never()).assertCanCreateQuizForCourse(any(), any());
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getData().isSaved()).isFalse();
        assertThat(response.getData().isPublished()).isFalse();
    }

    @Test
    void doesNotCallAiServiceWhenTeacherCannotCreateQuizForCourse() {
        UUID courseId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "oop.pdf", "application/pdf", "pdf".getBytes());
        when(courseQuizAuthorizationService.assertCanCreateQuizForCourse(courseId, AUTH_HEADER))
                .thenThrow(new UnauthorizedQuizAccessException("Teacher can only create quizzes for assigned courses"));

        assertThatThrownBy(() -> service.generateDraft(
                file,
                courseId,
                1,
                QuizGenerationDifficulty.MEDIUM,
                QuestionType.MULTIPLE_CHOICE_SINGLE_ANSWER,
                AUTH_HEADER
        )).isInstanceOf(UnauthorizedQuizAccessException.class);

        verify(aiQuizGenerationClient, never()).generateQuiz(any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsInvalidQuestionCountBeforeCallingAiService() {
        assertThatThrownBy(() -> service.generateDraft(
                new MockMultipartFile("file", "oop.pdf", "application/pdf", "pdf".getBytes()),
                UUID.randomUUID(),
                21,
                QuizGenerationDifficulty.MEDIUM,
                QuestionType.MULTIPLE_CHOICE_SINGLE_ANSWER,
                AUTH_HEADER
        )).isInstanceOf(InvalidQuestionOptionsException.class)
                .hasMessageContaining("Question count");

        verify(aiQuizGenerationClient, never()).generateQuiz(any(), any(), any(), any(), any(), any());
    }

    @Test
    void includesAiServiceErrorBodyInGenerationFailureMessage() {
        UUID courseId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", "pdf".getBytes());
        when(aiQuizGenerationClient.generateQuiz(
                eq(file),
                eq(courseId),
                eq(1),
                eq("MEDIUM"),
                eq("MULTIPLE_CHOICE_SINGLE_ANSWER"),
                eq(AUTH_HEADER)
        )).thenThrow(feignException(500, """
                {"status":500,"message":"Gemini API key is missing","timestamp":"2026-09-25T10:00:00"}
                """));

        assertThatThrownBy(() -> service.generateDraft(
                file,
                courseId,
                1,
                QuizGenerationDifficulty.MEDIUM,
                QuestionType.MULTIPLE_CHOICE_SINGLE_ANSWER,
                AUTH_HEADER
        )).isInstanceOf(AiQuizGenerationException.class)
                .hasMessage("AI quiz generation failed: Gemini API key is missing");
    }

    @Test
    void includesValidationErrorsFromAiServiceErrorBody() {
        UUID courseId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", "pdf".getBytes());
        when(aiQuizGenerationClient.generateQuiz(
                eq(file),
                eq(courseId),
                eq(1),
                eq("MEDIUM"),
                eq("MULTIPLE_CHOICE_SINGLE_ANSWER"),
                eq(AUTH_HEADER)
        )).thenThrow(feignException(400, """
                {"status":400,"message":"Validation failed","errors":{"file":"Document file is required"}}
                """));

        assertThatThrownBy(() -> service.generateDraft(
                file,
                courseId,
                1,
                QuizGenerationDifficulty.MEDIUM,
                QuestionType.MULTIPLE_CHOICE_SINGLE_ANSWER,
                AUTH_HEADER
        )).isInstanceOf(AiQuizGenerationException.class)
                .hasMessage("AI quiz generation request was rejected: Validation failed; file: Document file is required");
    }

    @Test
    void explainsNetworkFailureWhenAiServiceDoesNotRespond() {
        UUID courseId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "oop.pdf", "application/pdf", "pdf".getBytes());
        when(aiQuizGenerationClient.generateQuiz(
                eq(file),
                eq(courseId),
                eq(1),
                eq("MEDIUM"),
                eq("MULTIPLE_CHOICE_SINGLE_ANSWER"),
                eq(AUTH_HEADER)
        )).thenThrow(new RetryableException(
                -1,
                "Connection refused",
                Request.HttpMethod.POST,
                new IOException("Connection refused"),
                (Long) null,
                request()
        ));

        assertThatThrownBy(() -> service.generateDraft(
                file,
                courseId,
                1,
                QuizGenerationDifficulty.MEDIUM,
                QuestionType.MULTIPLE_CHOICE_SINGLE_ANSWER,
                AUTH_HEADER
        )).isInstanceOf(AiQuizGenerationException.class)
                .hasMessage("AI quiz generation failed: AI service is unreachable: Connection refused");
    }

    private AiGeneratedQuizDraftResponse generatedDraft(UUID courseId) {
        AiGeneratedQuizDraftResponse draft = new AiGeneratedQuizDraftResponse();
        draft.setCourseId(courseId);
        draft.setQuestionCount(1);
        draft.setDifficulty("MEDIUM");
        draft.setQuestionType("MULTIPLE_CHOICE_SINGLE_ANSWER");
        draft.setQuestions(List.of(question()));
        return draft;
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

    private FeignException feignException(int status, String body) {
        Response response = Response.builder()
                .status(status)
                .reason("AI service error")
                .request(request())
                .body(body, StandardCharsets.UTF_8)
                .build();
        return FeignException.errorStatus("AiQuizGenerationClient#generateQuiz", response);
    }

    private Request request() {
        return Request.create(
                Request.HttpMethod.POST,
                "/api/ai/quizzes/generate",
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8,
                null
        );
    }
}
