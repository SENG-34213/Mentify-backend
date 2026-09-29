package com.mentify.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.ai.config.AiProviderProperties;
import com.mentify.ai.dto.internal.AiExecutionRequest;
import com.mentify.ai.dto.request.AiChatRequest;
import com.mentify.ai.dto.response.AiChatResponse;
import com.mentify.ai.dto.response.AiGenerateResponse;
import com.mentify.ai.dto.tool.QuizPerformanceSummary;
import com.mentify.ai.dto.tool.TodayQuizPerformanceToolResult;
import com.mentify.ai.enums.AiFeatureType;
import com.mentify.ai.exception.AiProviderUnavailableException;
import com.mentify.ai.provider.AiProvider;
import com.mentify.ai.security.AuthenticatedUserService;
import com.mentify.ai.service.AiAuditService;
import com.mentify.ai.service.AiContentGuardService;
import com.mentify.ai.service.AiUsageGuardService;
import com.mentify.ai.tool.TodayQuizPerformanceTool;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    private static final String AUTH_HEADER = "Bearer token";

    @Mock
    private AiProvider aiProvider;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private AiUsageGuardService usageGuardService;

    @Mock
    private AiContentGuardService contentGuardService;

    @Mock
    private AiAuditService auditService;

    @Mock
    private TodayQuizPerformanceTool todayQuizPerformanceTool;

    private AiChatServiceImpl aiChatService;

    @BeforeEach
    void setUp() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.getProvider().setName("OPENAI");

        lenient().when(aiProvider.getProviderName()).thenReturn("OPENAI");
        lenient().when(todayQuizPerformanceTool.getName()).thenReturn(TodayQuizPerformanceTool.TOOL_NAME);

        aiChatService = new AiChatServiceImpl(
                properties,
                List.of(aiProvider),
                authenticatedUserService,
                usageGuardService,
                contentGuardService,
                auditService,
                todayQuizPerformanceTool,
                new ObjectMapper().findAndRegisterModules()
        );
    }

    @Test
    void teacherQuestionAboutTodayQuizUsesToolAndGroundedAiResponse() {
        UUID teacherId = UUID.randomUUID();
        when(authenticatedUserService.getCurrentUserId()).thenReturn(teacherId);
        when(contentGuardService.sanitizeForPrompt(eq(AiFeatureType.TUTOR_CHAT), eq("ai_chat_message"), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        when(todayQuizPerformanceTool.execute(AUTH_HEADER)).thenReturn(singleQuizResult());
        when(aiProvider.generate(any(AiExecutionRequest.class))).thenReturn(AiGenerateResponse.builder()
                .content("Today's Java OOP Quiz had 2 submitted attempts with an average score of 70.00%.")
                .provider("OPENAI")
                .model("gpt-test")
                .generatedAt(LocalDateTime.now())
                .build());

        AiChatResponse response = aiChatService.chat(
                AiChatRequest.builder().message("How was today's quiz performance?").build(),
                AUTH_HEADER
        );

        assertThat(response.getMessage()).contains("Java OOP Quiz");
        assertThat(response.getToolsUsed()).containsExactly(TodayQuizPerformanceTool.TOOL_NAME);
        verify(todayQuizPerformanceTool).execute(AUTH_HEADER);

        ArgumentCaptor<AiExecutionRequest> executionCaptor = ArgumentCaptor.forClass(AiExecutionRequest.class);
        verify(aiProvider).generate(executionCaptor.capture());
        assertThat(executionCaptor.getValue().getUserInput()).contains("getTodayQuizPerformance", "Java OOP Quiz");
    }

    @Test
    void toolForbiddenResponseIsRejectedAsAccessDenied() {
        UUID teacherId = UUID.randomUUID();
        when(authenticatedUserService.getCurrentUserId()).thenReturn(teacherId);
        when(contentGuardService.sanitizeForPrompt(eq(AiFeatureType.TUTOR_CHAT), eq("ai_chat_message"), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        when(todayQuizPerformanceTool.execute(AUTH_HEADER)).thenThrow(forbiddenFeignException());

        assertThatThrownBy(() -> aiChatService.chat(
                AiChatRequest.builder().message("How was today's quiz performance?").build(),
                AUTH_HEADER
        )).isInstanceOf(AccessDeniedException.class)
                .hasMessage("Not authorized to access quiz performance data");
    }

    @Test
    void noQuizDataDoesNotCallAiProviderOrFabricateResults() {
        UUID teacherId = UUID.randomUUID();
        when(authenticatedUserService.getCurrentUserId()).thenReturn(teacherId);
        when(contentGuardService.sanitizeForPrompt(eq(AiFeatureType.TUTOR_CHAT), eq("ai_chat_message"), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        when(todayQuizPerformanceTool.execute(AUTH_HEADER)).thenReturn(TodayQuizPerformanceToolResult.builder()
                .date(LocalDate.now())
                .resultType("NO_QUIZZES")
                .quizzes(List.of())
                .build());

        AiChatResponse response = aiChatService.chat(
                AiChatRequest.builder().message("How was today's quiz performance?").build(),
                AUTH_HEADER
        );

        assertThat(response.getMessage()).isEqualTo("I couldn't find any published quizzes scheduled for today.");
        assertThat(response.getToolsUsed()).containsExactly(TodayQuizPerformanceTool.TOOL_NAME);
        verify(aiProvider, never()).generate(any());
    }

    @Test
    void multipleQuizzesAskClarificationWhenUserDidNotAskForOverallSummary() {
        UUID teacherId = UUID.randomUUID();
        when(authenticatedUserService.getCurrentUserId()).thenReturn(teacherId);
        when(contentGuardService.sanitizeForPrompt(eq(AiFeatureType.TUTOR_CHAT), eq("ai_chat_message"), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        when(todayQuizPerformanceTool.execute(AUTH_HEADER)).thenReturn(TodayQuizPerformanceToolResult.builder()
                .date(LocalDate.now())
                .resultType("MULTIPLE_QUIZZES")
                .quizzes(List.of(
                        QuizPerformanceSummary.builder().quizTitle("Java OOP").build(),
                        QuizPerformanceSummary.builder().quizTitle("SQL Basics").build()
                ))
                .build());

        AiChatResponse response = aiChatService.chat(
                AiChatRequest.builder().message("How was today's quiz?").build(),
                AUTH_HEADER
        );

        assertThat(response.getMessage()).contains("multiple quizzes", "Java OOP", "SQL Basics");
        verify(aiProvider, never()).generate(any());
    }

    @Test
    void generalQuestionDoesNotUseDatabaseTool() {
        UUID teacherId = UUID.randomUUID();
        when(authenticatedUserService.getCurrentUserId()).thenReturn(teacherId);
        when(contentGuardService.sanitizeForPrompt(eq(AiFeatureType.TUTOR_CHAT), eq("ai_chat_message"), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        when(aiProvider.generate(any(AiExecutionRequest.class))).thenReturn(AiGenerateResponse.builder()
                .content("Encapsulation keeps data and behavior together while controlling access.")
                .provider("OPENAI")
                .model("gpt-test")
                .generatedAt(LocalDateTime.now())
                .build());

        AiChatResponse response = aiChatService.chat(
                AiChatRequest.builder().message("What is encapsulation?").build(),
                AUTH_HEADER
        );

        assertThat(response.getMessage()).contains("Encapsulation");
        assertThat(response.getToolsUsed()).isEmpty();
        verify(todayQuizPerformanceTool, never()).execute(any());
    }

    @Test
    void aiProviderFailureIsPropagatedForStandardHandler() {
        UUID teacherId = UUID.randomUUID();
        when(authenticatedUserService.getCurrentUserId()).thenReturn(teacherId);
        when(contentGuardService.sanitizeForPrompt(eq(AiFeatureType.TUTOR_CHAT), eq("ai_chat_message"), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        when(aiProvider.generate(any(AiExecutionRequest.class)))
                .thenThrow(new AiProviderUnavailableException("AI provider is unavailable"));

        assertThatThrownBy(() -> aiChatService.chat(
                AiChatRequest.builder().message("What is polymorphism?").build(),
                AUTH_HEADER
        )).isInstanceOf(AiProviderUnavailableException.class)
                .hasMessage("AI provider is unavailable");
    }

    private TodayQuizPerformanceToolResult singleQuizResult() {
        return TodayQuizPerformanceToolResult.builder()
                .date(LocalDate.now())
                .resultType("SINGLE_QUIZ")
                .quizzes(List.of(QuizPerformanceSummary.builder()
                        .quizId(UUID.randomUUID())
                        .quizTitle("Java OOP Quiz")
                        .submittedAttempts(2)
                        .participants(2)
                        .averagePercentage(new BigDecimal("70.00"))
                        .highestPercentage(new BigDecimal("80.00"))
                        .lowestPercentage(new BigDecimal("60.00"))
                        .passedAttempts(2)
                        .belowPassThresholdAttempts(0)
                        .build()))
                .build();
    }

    private FeignException.Forbidden forbiddenFeignException() {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/api/internal/quizzes/analytics/today/performance",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                null
        );
        return new FeignException.Forbidden("Forbidden", request, null, Map.of());
    }
}
