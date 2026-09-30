package com.mentify.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.ai.config.AiProviderProperties;
import com.mentify.ai.dto.internal.AiExecutionRequest;
import com.mentify.ai.dto.request.AiChatRequest;
import com.mentify.ai.dto.response.AiChatResponse;
import com.mentify.ai.dto.response.AiGenerateResponse;
import com.mentify.ai.dto.tool.QuizPerformanceSummary;
import com.mentify.ai.dto.tool.TodayQuizPerformanceToolResult;
import com.mentify.ai.dto.tool.UserRegistrationOverviewToolResult;
import com.mentify.ai.entity.AiConversation;
import com.mentify.ai.entity.AiMessage;
import com.mentify.ai.enums.AiFeatureType;
import com.mentify.ai.enums.AiMessageRole;
import com.mentify.ai.exception.AiConversationNotFoundException;
import com.mentify.ai.exception.AiProviderUnavailableException;
import com.mentify.ai.provider.AiProvider;
import com.mentify.ai.repository.AiConversationRepository;
import com.mentify.ai.repository.AiMessageRepository;
import com.mentify.ai.security.AuthenticatedUserService;
import com.mentify.ai.service.AiAuditService;
import com.mentify.ai.service.AiContentGuardService;
import com.mentify.ai.service.AiUsageGuardService;
import com.mentify.ai.tool.TodayQuizPerformanceTool;
import com.mentify.ai.tool.UserRegistrationOverviewTool;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
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

    @Mock
    private UserRegistrationOverviewTool userRegistrationOverviewTool;

    @Mock
    private AiConversationRepository conversationRepository;

    @Mock
    private AiMessageRepository messageRepository;

    private AiChatServiceImpl aiChatService;

    @BeforeEach
    void setUp() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.getProvider().setName("OPENAI");

        lenient().when(aiProvider.getProviderName()).thenReturn("OPENAI");
        lenient().when(todayQuizPerformanceTool.getName()).thenReturn(TodayQuizPerformanceTool.TOOL_NAME);
        lenient().when(userRegistrationOverviewTool.getName()).thenReturn(UserRegistrationOverviewTool.TOOL_NAME);

        aiChatService = new AiChatServiceImpl(
                properties,
                List.of(aiProvider),
                authenticatedUserService,
                usageGuardService,
                contentGuardService,
                auditService,
                todayQuizPerformanceTool,
                userRegistrationOverviewTool,
                new ObjectMapper().findAndRegisterModules(),
                conversationRepository,
                messageRepository
        );
    }

    @Test
    void teacherQuestionAboutTodayQuizUsesToolAndGroundedAiResponse() {
        UUID teacherId = UUID.randomUUID();
        UUID conversationId = prepareOwnedConversation(teacherId);
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
                AiChatRequest.builder()
                        .conversationId(conversationId)
                        .message("How was today's quiz performance?")
                        .build(),
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
        UUID conversationId = prepareOwnedConversation(teacherId);
        when(authenticatedUserService.getCurrentUserId()).thenReturn(teacherId);
        when(contentGuardService.sanitizeForPrompt(eq(AiFeatureType.TUTOR_CHAT), eq("ai_chat_message"), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        when(todayQuizPerformanceTool.execute(AUTH_HEADER)).thenThrow(forbiddenFeignException());

        assertThatThrownBy(() -> aiChatService.chat(
                AiChatRequest.builder()
                        .conversationId(conversationId)
                        .message("How was today's quiz performance?")
                        .build(),
                AUTH_HEADER
        )).isInstanceOf(AccessDeniedException.class)
                .hasMessage("Not authorized to access quiz performance data");
    }

    @Test
    void noQuizDataDoesNotCallAiProviderOrFabricateResults() {
        UUID teacherId = UUID.randomUUID();
        UUID conversationId = prepareOwnedConversation(teacherId);
        when(authenticatedUserService.getCurrentUserId()).thenReturn(teacherId);
        when(contentGuardService.sanitizeForPrompt(eq(AiFeatureType.TUTOR_CHAT), eq("ai_chat_message"), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        when(todayQuizPerformanceTool.execute(AUTH_HEADER)).thenReturn(TodayQuizPerformanceToolResult.builder()
                .date(LocalDate.now())
                .resultType("NO_QUIZZES")
                .quizzes(List.of())
                .build());

        AiChatResponse response = aiChatService.chat(
                AiChatRequest.builder()
                        .conversationId(conversationId)
                        .message("How was today's quiz performance?")
                        .build(),
                AUTH_HEADER
        );

        assertThat(response.getMessage()).isEqualTo("I couldn't find any published quizzes scheduled for today.");
        assertThat(response.getToolsUsed()).containsExactly(TodayQuizPerformanceTool.TOOL_NAME);
        verify(aiProvider, never()).generate(any());
    }

    @Test
    void multipleQuizzesAskClarificationWhenUserDidNotAskForOverallSummary() {
        UUID teacherId = UUID.randomUUID();
        UUID conversationId = prepareOwnedConversation(teacherId);
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
                AiChatRequest.builder()
                        .conversationId(conversationId)
                        .message("How was today's quiz?")
                        .build(),
                AUTH_HEADER
        );

        assertThat(response.getMessage()).contains("multiple quizzes", "Java OOP", "SQL Basics");
        verify(aiProvider, never()).generate(any());
    }

    @Test
    void generalQuestionDoesNotUseDatabaseTool() {
        UUID teacherId = UUID.randomUUID();
        UUID conversationId = prepareOwnedConversation(teacherId);
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
                AiChatRequest.builder()
                        .conversationId(conversationId)
                        .message("What is encapsulation?")
                        .build(),
                AUTH_HEADER
        );

        assertThat(response.getMessage()).contains("Encapsulation");
        assertThat(response.getToolsUsed()).isEmpty();
        verify(todayQuizPerformanceTool, never()).execute(any());
        verify(userRegistrationOverviewTool, never()).execute(any());
    }

    @Test
    void adminRegistrationOverviewUsesAdminToolAndGroundedAiResponse() {
        UUID adminId = UUID.randomUUID();
        UUID conversationId = prepareOwnedConversation(adminId);
        when(authenticatedUserService.getCurrentUserId()).thenReturn(adminId);
        when(contentGuardService.sanitizeForPrompt(eq(AiFeatureType.TUTOR_CHAT), eq("ai_chat_message"), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        when(userRegistrationOverviewTool.execute(AUTH_HEADER)).thenReturn(userOverviewResult());
        when(aiProvider.generate(any(AiExecutionRequest.class))).thenReturn(AiGenerateResponse.builder()
                .content("Mentify has 15 users: 10 students and 3 teachers.")
                .provider("OPENAI")
                .model("gpt-test")
                .generatedAt(LocalDateTime.now())
                .build());

        AiChatResponse response = aiChatService.chat(
                AiChatRequest.builder()
                        .conversationId(conversationId)
                        .message("Show registered student teacher details overview")
                        .build(),
                AUTH_HEADER
        );

        assertThat(response.getMessage()).contains("15 users");
        assertThat(response.getToolsUsed()).containsExactly(UserRegistrationOverviewTool.TOOL_NAME);
        verify(userRegistrationOverviewTool).execute(AUTH_HEADER);
        verify(todayQuizPerformanceTool, never()).execute(any());
    }

    @Test
    void adminUserCountQuestionWithTypoUsesAdminTool() {
        UUID adminId = UUID.randomUUID();
        UUID conversationId = prepareOwnedConversation(adminId);
        when(authenticatedUserService.getCurrentUserId()).thenReturn(adminId);
        when(contentGuardService.sanitizeForPrompt(eq(AiFeatureType.TUTOR_CHAT), eq("ai_chat_message"), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        when(userRegistrationOverviewTool.execute(AUTH_HEADER)).thenReturn(userOverviewResult());
        when(aiProvider.generate(any(AiExecutionRequest.class))).thenReturn(AiGenerateResponse.builder()
                .content("Mentify has 15 registered users.")
                .provider("OPENAI")
                .model("gpt-test")
                .generatedAt(LocalDateTime.now())
                .build());

        AiChatResponse response = aiChatService.chat(
                AiChatRequest.builder()
                        .conversationId(conversationId)
                        .message("how many user are regosterd?")
                        .build(),
                AUTH_HEADER
        );

        assertThat(response.getMessage()).contains("15 registered users");
        assertThat(response.getToolsUsed()).containsExactly(UserRegistrationOverviewTool.TOOL_NAME);
        verify(userRegistrationOverviewTool).execute(AUTH_HEADER);
        verify(todayQuizPerformanceTool, never()).execute(any());
    }

    @Test
    void nonAdminUserRegistrationOverviewIsRejectedAsAccessDenied() {
        UUID teacherId = UUID.randomUUID();
        UUID conversationId = prepareOwnedConversation(teacherId);
        when(authenticatedUserService.getCurrentUserId()).thenReturn(teacherId);
        when(contentGuardService.sanitizeForPrompt(eq(AiFeatureType.TUTOR_CHAT), eq("ai_chat_message"), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        when(userRegistrationOverviewTool.execute(AUTH_HEADER)).thenThrow(forbiddenFeignException());

        assertThatThrownBy(() -> aiChatService.chat(
                AiChatRequest.builder()
                        .conversationId(conversationId)
                        .message("How many registered students and teachers are there?")
                        .build(),
                AUTH_HEADER
        )).isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only admins can access user registration overview data");
    }

    @Test
    void aiProviderFailureIsPropagatedForStandardHandler() {
        UUID teacherId = UUID.randomUUID();
        UUID conversationId = prepareOwnedConversation(teacherId);
        when(authenticatedUserService.getCurrentUserId()).thenReturn(teacherId);
        when(contentGuardService.sanitizeForPrompt(eq(AiFeatureType.TUTOR_CHAT), eq("ai_chat_message"), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        when(aiProvider.generate(any(AiExecutionRequest.class)))
                .thenThrow(new AiProviderUnavailableException("AI provider is unavailable"));

        assertThatThrownBy(() -> aiChatService.chat(
                AiChatRequest.builder()
                        .conversationId(conversationId)
                        .message("What is polymorphism?")
                        .build(),
                AUTH_HEADER
        )).isInstanceOf(AiProviderUnavailableException.class)
                .hasMessage("AI provider is unavailable");
    }

    @Test
    void followUpQuestionUsesRecentConversationHistoryAndAuthorizedTool() {
        UUID adminId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        AiConversation conversation = conversation(adminId, conversationId);
        AiMessage currentQuestion = message(conversation, AiMessageRole.USER, "How many of them are active?");
        AiMessage previousAnswer = message(conversation, AiMessageRole.ASSISTANT, "Mentify has 3 registered teachers.");
        AiMessage previousQuestion = message(conversation, AiMessageRole.USER, "How many teachers are registered?");

        prepareOwnedConversation(adminId, conversationId, List.of(currentQuestion, previousAnswer, previousQuestion));
        when(authenticatedUserService.getCurrentUserId()).thenReturn(adminId);
        when(contentGuardService.sanitizeForPrompt(eq(AiFeatureType.TUTOR_CHAT), eq("ai_chat_message"), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        when(userRegistrationOverviewTool.execute(AUTH_HEADER)).thenReturn(userOverviewResult());
        when(aiProvider.generate(any(AiExecutionRequest.class))).thenReturn(AiGenerateResponse.builder()
                .content("There are 9 active users. The available tool data does not break active counts down by teacher.")
                .provider("OPENAI")
                .model("gpt-test")
                .generatedAt(LocalDateTime.now())
                .build());

        AiChatResponse response = aiChatService.chat(
                AiChatRequest.builder()
                        .conversationId(conversationId)
                        .message("How many of them are active?")
                        .build(),
                AUTH_HEADER
        );

        assertThat(response.getToolsUsed()).containsExactly(UserRegistrationOverviewTool.TOOL_NAME);
        verify(userRegistrationOverviewTool).execute(AUTH_HEADER);

        ArgumentCaptor<AiExecutionRequest> executionCaptor = ArgumentCaptor.forClass(AiExecutionRequest.class);
        verify(aiProvider).generate(executionCaptor.capture());
        assertThat(executionCaptor.getValue().getUserInput())
                .contains("How many teachers are registered?", "Mentify has 3 registered teachers.", "How many of them are active?");
    }

    @Test
    void invalidConversationIdIsRejectedBeforeMessagePersistence() {
        UUID teacherId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        when(authenticatedUserService.getCurrentUserId()).thenReturn(teacherId);
        when(contentGuardService.sanitizeForPrompt(eq(AiFeatureType.TUTOR_CHAT), eq("ai_chat_message"), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        when(conversationRepository.findByIdAndUserId(conversationId, teacherId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiChatService.chat(
                AiChatRequest.builder()
                        .conversationId(conversationId)
                        .message("What is inheritance?")
                        .build(),
                AUTH_HEADER
        )).isInstanceOf(AiConversationNotFoundException.class);

        verify(messageRepository, never()).saveAndFlush(any(AiMessage.class));
        verify(aiProvider, never()).generate(any());
    }

    @Test
    void unauthorizedConversationAccessIsRejectedBeforeMessagePersistence() {
        UUID teacherId = UUID.randomUUID();
        UUID otherUsersConversationId = UUID.randomUUID();
        when(authenticatedUserService.getCurrentUserId()).thenReturn(teacherId);
        when(contentGuardService.sanitizeForPrompt(eq(AiFeatureType.TUTOR_CHAT), eq("ai_chat_message"), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        when(conversationRepository.findByIdAndUserId(otherUsersConversationId, teacherId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiChatService.chat(
                AiChatRequest.builder()
                        .conversationId(otherUsersConversationId)
                        .message("What is inheritance?")
                        .build(),
                AUTH_HEADER
        )).isInstanceOf(AiConversationNotFoundException.class);

        verify(messageRepository, never()).saveAndFlush(any(AiMessage.class));
        verify(aiProvider, never()).generate(any());
    }

    @Test
    void chatPersistsUserAndAssistantMessagesAndTouchesConversation() {
        UUID teacherId = UUID.randomUUID();
        UUID conversationId = prepareOwnedConversation(teacherId);
        when(authenticatedUserService.getCurrentUserId()).thenReturn(teacherId);
        when(contentGuardService.sanitizeForPrompt(eq(AiFeatureType.TUTOR_CHAT), eq("ai_chat_message"), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        when(aiProvider.generate(any(AiExecutionRequest.class))).thenReturn(AiGenerateResponse.builder()
                .content("Polymorphism lets one interface support multiple implementations.")
                .provider("OPENAI")
                .model("gpt-test")
                .generatedAt(LocalDateTime.now())
                .build());

        aiChatService.chat(
                AiChatRequest.builder()
                        .conversationId(conversationId)
                        .message("What is polymorphism?")
                        .build(),
                AUTH_HEADER
        );

        ArgumentCaptor<AiMessage> messageCaptor = ArgumentCaptor.forClass(AiMessage.class);
        verify(messageRepository, times(2)).saveAndFlush(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues())
                .extracting(AiMessage::getRole)
                .containsExactly(AiMessageRole.USER, AiMessageRole.ASSISTANT);
        assertThat(messageCaptor.getAllValues())
                .extracting(AiMessage::getContent)
                .containsExactly(
                        "What is polymorphism?",
                        "Polymorphism lets one interface support multiple implementations."
                );
        verify(conversationRepository).saveAndFlush(any(AiConversation.class));
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

    private UserRegistrationOverviewToolResult userOverviewResult() {
        return UserRegistrationOverviewToolResult.builder()
                .totalUsers(15)
                .usersByRole(Map.of("STUDENT", 10L, "TEACHER", 3L, "ADMIN", 2L))
                .usersByAccountStatus(Map.of("ACTIVE", 9L, "INVITED", 6L))
                .recentStudents(List.of(UserRegistrationOverviewToolResult.RegisteredUserSummary.builder()
                        .firstName("Nimal")
                        .lastName("Perera")
                        .email("nimal@example.com")
                        .role("STUDENT")
                        .accountStatus("ACTIVE")
                        .studentId("TIT-03-001")
                        .grade("03")
                        .build()))
                .recentTeachers(List.of(UserRegistrationOverviewToolResult.RegisteredUserSummary.builder()
                        .firstName("Amal")
                        .lastName("Silva")
                        .email("amal@example.com")
                        .role("TEACHER")
                        .accountStatus("INVITED")
                        .teacherCode("TIT-TCH-001")
                        .specializations(List.of("Math"))
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

    private UUID prepareOwnedConversation(UUID userId) {
        UUID conversationId = UUID.randomUUID();
        prepareOwnedConversation(userId, conversationId, List.of());
        return conversationId;
    }

    private void prepareOwnedConversation(UUID userId, UUID conversationId, List<AiMessage> recentMessagesDescending) {
        AiConversation conversation = conversation(userId, conversationId);
        lenient().when(conversationRepository.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.of(conversation));
        lenient().when(messageRepository.findByConversation_IdOrderByCreatedAtDesc(eq(conversationId), any()))
                .thenReturn(recentMessagesDescending);
        lenient().when(messageRepository.saveAndFlush(any(AiMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(conversationRepository.saveAndFlush(any(AiConversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private AiConversation conversation(UUID userId, UUID conversationId) {
        AiConversation conversation = AiConversation.builder()
                .userId(userId)
                .title("AI conversation")
                .build();
        conversation.setId(conversationId);
        return conversation;
    }

    private AiMessage message(AiConversation conversation, AiMessageRole role, String content) {
        AiMessage message = AiMessage.builder()
                .conversation(conversation)
                .role(role)
                .content(content)
                .build();
        message.setId(UUID.randomUUID());
        return message;
    }
}
