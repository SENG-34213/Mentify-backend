package com.mentify.ai.service.impl;

import com.mentify.ai.config.AiProviderProperties;
import com.mentify.ai.dto.internal.AiExecutionRequest;
import com.mentify.ai.dto.request.AiChatRequest;
import com.mentify.ai.dto.request.AiGenerateRequest;
import com.mentify.ai.dto.response.AiChatResponse;
import com.mentify.ai.dto.response.AiGenerateResponse;
import com.mentify.ai.enums.AiFeatureType;
import com.mentify.ai.provider.AiProvider;
import com.mentify.ai.security.AuthenticatedUserService;
import com.mentify.ai.service.AiAuditService;
import com.mentify.ai.service.AiChatService;
import com.mentify.ai.service.AiContentGuardService;
import com.mentify.ai.service.AiUsageGuardService;
import com.mentify.ai.tool.UserRegistrationOverviewTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {

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
    private AiChatService aiChatService;

    private AiServiceImpl aiService;

    @BeforeEach
    void setUp() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.getProvider().setName("OPENAI");

        lenient().when(aiProvider.getProviderName()).thenReturn("OPENAI");

        aiService = new AiServiceImpl(
                properties,
                List.of(aiProvider),
                authenticatedUserService,
                usageGuardService,
                contentGuardService,
                auditService,
                aiChatService
        );
    }

    @Test
    void generateRoutesRegisteredUserCountQuestionToChatTools() {
        when(aiChatService.chat(any(AiChatRequest.class), eq(AUTH_HEADER))).thenReturn(AiChatResponse.builder()
                .message("Mentify has 15 registered users.")
                .toolsUsed(List.of(UserRegistrationOverviewTool.TOOL_NAME))
                .timestamp(LocalDateTime.now())
                .build());

        AiGenerateResponse response = aiService.generate(
                new AiGenerateRequest("How many users are registered?"),
                AUTH_HEADER
        );

        assertThat(response.getContent()).isEqualTo("Mentify has 15 registered users.");
        assertThat(response.getProvider()).isEqualTo("MENTIFY_CHAT");
        assertThat(response.getModel()).isEqualTo(UserRegistrationOverviewTool.TOOL_NAME);
        verify(aiProvider, never()).generate(any());
        verify(usageGuardService, never()).assertAllowed(any(), eq(AiFeatureType.GENERAL_GENERATION));
    }

    @Test
    void generateRoutesRecentStudentTeacherRegistrationsToChatTools() {
        when(aiChatService.chat(any(AiChatRequest.class), eq(AUTH_HEADER))).thenReturn(AiChatResponse.builder()
                .message("Recent registrations include 1 student and 1 teacher.")
                .toolsUsed(List.of(UserRegistrationOverviewTool.TOOL_NAME))
                .timestamp(LocalDateTime.now())
                .build());

        AiGenerateResponse response = aiService.generate(
                new AiGenerateRequest("Show recent student and teacher registrations."),
                AUTH_HEADER
        );

        assertThat(response.getContent()).contains("Recent registrations");
        ArgumentCaptor<AiChatRequest> requestCaptor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(aiChatService).chat(requestCaptor.capture(), eq(AUTH_HEADER));
        assertThat(requestCaptor.getValue().getMessage()).isEqualTo("Show recent student and teacher registrations.");
        verify(aiProvider, never()).generate(any());
    }

    @Test
    void generateKeepsGeneralPromptsOnGeneralProvider() {
        UUID adminId = UUID.randomUUID();
        when(authenticatedUserService.getCurrentUserId()).thenReturn(adminId);
        when(contentGuardService.sanitizeForPrompt(eq(AiFeatureType.GENERAL_GENERATION), eq("generic_prompt"), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        when(aiProvider.generate(any(AiExecutionRequest.class))).thenReturn(AiGenerateResponse.builder()
                .content("Inheritance allows a class to reuse behavior from another class.")
                .provider("OPENAI")
                .model("gpt-test")
                .generatedAt(LocalDateTime.now())
                .build());

        AiGenerateResponse response = aiService.generate(
                new AiGenerateRequest("Explain inheritance"),
                AUTH_HEADER
        );

        assertThat(response.getContent()).contains("Inheritance");
        verify(aiChatService, never()).chat(any(), any());
        verify(aiProvider).generate(any());
    }
}
