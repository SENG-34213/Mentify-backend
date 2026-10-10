package com.mentify.ai.service.impl;

import com.mentify.ai.client.LessonServiceClient;
import com.mentify.ai.config.AiProviderProperties;
import com.mentify.ai.dto.internal.AiExecutionRequest;
import com.mentify.ai.dto.response.AiGenerateResponse;
import com.mentify.ai.dto.response.InternalLessonResponse;
import com.mentify.ai.dto.response.LessonSummaryResponse;
import com.mentify.ai.exception.AiEmptyResponseException;
import com.mentify.ai.exception.AiProviderConfigurationException;
import com.mentify.ai.prompt.AiPromptRegistry;
import com.mentify.ai.provider.AiProvider;
import com.mentify.ai.security.AuthenticatedUserService;
import com.mentify.ai.service.AiAuditService;
import com.mentify.ai.service.AiContentGuardService;
import com.mentify.ai.service.AiUsageGuardService;
import com.mentify.payload.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonSummarizationServiceImplTest {

    @Mock
    private LessonServiceClient lessonServiceClient;

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

    private AiProviderProperties properties;
    private AiPromptRegistry promptRegistry;

    private LessonSummarizationServiceImpl lessonSummarizationService;

    @BeforeEach
    void setUp() {
        properties = new AiProviderProperties();
        properties.getProvider().setName("GEMINI");
        promptRegistry = new AiPromptRegistry();
        
        lessonSummarizationService = new LessonSummarizationServiceImpl(
                lessonServiceClient,
                List.of(aiProvider),
                properties,
                authenticatedUserService,
                promptRegistry,
                usageGuardService,
                contentGuardService,
                auditService
        );
    }

    @Test
    void summarizeLesson_ShouldReturnSummary_WhenValidLessonProvided() {
        // Arrange
        when(aiProvider.getProviderName()).thenReturn("GEMINI");
        UUID lessonId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String authHeader = "Bearer test-token";
        InternalLessonResponse lessonData = InternalLessonResponse.builder()
                .id(lessonId)
                .title("Test Lesson")
                .description("Test content")
                .build();
        
        ApiResponse<InternalLessonResponse> apiResponse = ApiResponse.<InternalLessonResponse>builder()
                .data(lessonData)
                .build();
        
        when(lessonServiceClient.getLessonForAi(eq(lessonId), eq(authHeader))).thenReturn(apiResponse);
        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(contentGuardService.sanitizeForPrompt(any(), eq("lesson_title"), eq("Test Lesson")))
                .thenReturn("<untrusted_lesson_title>\nTest Lesson\n</untrusted_lesson_title>");
        when(contentGuardService.sanitizeForPrompt(any(), eq("lesson_content"), eq("Test content")))
                .thenReturn("<untrusted_lesson_content>\nTest content\n</untrusted_lesson_content>");
        
        AiGenerateResponse generateResponse = AiGenerateResponse.builder()
                .content("This is a summary")
                .provider("GEMINI")
                .model("gemini-3-flash-preview")
                .generatedAt(LocalDateTime.now())
                .build();
        
        when(aiProvider.generate(any(AiExecutionRequest.class))).thenReturn(generateResponse);

        // Act
        LessonSummaryResponse response = lessonSummarizationService.summarizeLesson(lessonId, authHeader);

        // Assert
        assertNotNull(response);
        assertEquals(lessonId, response.getLessonId());
        assertEquals("This is a summary", response.getSummary());
        assertEquals("GEMINI", response.getProvider());
        verify(usageGuardService).assertAllowed(userId, com.mentify.ai.enums.AiFeatureType.LESSON_SUMMARIZATION);
        verify(auditService).recordCompleted(any(), eq(com.mentify.ai.enums.AiFeatureType.LESSON_SUMMARIZATION),
                eq(userId), eq(null), eq(lessonId), eq(generateResponse));
    }

    @Test
    void summarizeLesson_ShouldUseVersionedLessonSummaryPrompt() {
        // Arrange
        when(aiProvider.getProviderName()).thenReturn("GEMINI");
        UUID lessonId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String authHeader = "Bearer test-token";
        InternalLessonResponse lessonData = InternalLessonResponse.builder()
                .id(lessonId)
                .title("Encapsulation")
                .description("Hide internal object state")
                .build();

        ApiResponse<InternalLessonResponse> apiResponse = ApiResponse.<InternalLessonResponse>builder()
                .data(lessonData)
                .build();

        when(lessonServiceClient.getLessonForAi(eq(lessonId), eq(authHeader))).thenReturn(apiResponse);
        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(contentGuardService.sanitizeForPrompt(any(), eq("lesson_title"), eq("Encapsulation")))
                .thenReturn("<untrusted_lesson_title>\nEncapsulation\n</untrusted_lesson_title>");
        when(contentGuardService.sanitizeForPrompt(any(), eq("lesson_content"), eq("Hide internal object state")))
                .thenReturn("<untrusted_lesson_content>\nHide internal object state\n</untrusted_lesson_content>");
        when(aiProvider.generate(any(AiExecutionRequest.class))).thenReturn(AiGenerateResponse.builder()
                .content("Summary")
                .provider("GEMINI")
                .model("gemini-3-flash-preview")
                .generatedAt(LocalDateTime.now())
                .build());

        var requestCaptor = forClass(AiExecutionRequest.class);

        // Act
        lessonSummarizationService.summarizeLesson(lessonId, authHeader);

        // Assert
        org.mockito.Mockito.verify(aiProvider).generate(requestCaptor.capture());
        AiExecutionRequest capturedRequest = requestCaptor.getValue();
        assertEquals(promptRegistry.get(AiPromptRegistry.LESSON_SUMMARY, AiPromptRegistry.V1).systemPrompt(),
                capturedRequest.getSystemPrompt());
        assertEquals("Title: <untrusted_lesson_title>\nEncapsulation\n</untrusted_lesson_title>\n\nContent:\n<untrusted_lesson_content>\nHide internal object state\n</untrusted_lesson_content>", capturedRequest.getUserInput());
    }

    @Test
    void summarizeLesson_ShouldThrowException_WhenLessonContentIsEmpty() {
        // Arrange
        UUID lessonId = UUID.randomUUID();
        String authHeader = "Bearer test-token";
        InternalLessonResponse lessonData = InternalLessonResponse.builder()
                .id(lessonId)
                .title("Test Lesson")
                .description("")
                .build();
        
        ApiResponse<InternalLessonResponse> apiResponse = ApiResponse.<InternalLessonResponse>builder()
                .data(lessonData)
                .build();
        
        when(lessonServiceClient.getLessonForAi(eq(lessonId), eq(authHeader))).thenReturn(apiResponse);
        when(authenticatedUserService.getCurrentUserId()).thenReturn(UUID.randomUUID());

        // Act & Assert
        assertThrows(AiEmptyResponseException.class, () -> 
            lessonSummarizationService.summarizeLesson(lessonId, authHeader)
        );
    }

    @Test
    void summarizeLesson_ShouldThrowException_WhenProviderNameMismatch() {
        // Arrange
        when(aiProvider.getProviderName()).thenReturn("GEMINI"); // Need this to avoid NPE in filter
        properties.getProvider().setName("OPENAI");
        UUID lessonId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String authHeader = "Bearer test-token";

        InternalLessonResponse lessonData = InternalLessonResponse.builder()
                .id(lessonId)
                .title("Test Lesson")
                .description("Test content")
                .build();
        ApiResponse<InternalLessonResponse> apiResponse = ApiResponse.<InternalLessonResponse>builder()
                .data(lessonData)
                .build();
        when(lessonServiceClient.getLessonForAi(eq(lessonId), eq(authHeader))).thenReturn(apiResponse);
        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(contentGuardService.sanitizeForPrompt(any(), eq("lesson_title"), eq("Test Lesson")))
                .thenReturn("<untrusted_lesson_title>\nTest Lesson\n</untrusted_lesson_title>");
        when(contentGuardService.sanitizeForPrompt(any(), eq("lesson_content"), eq("Test content")))
                .thenReturn("<untrusted_lesson_content>\nTest content\n</untrusted_lesson_content>");

        // Act & Assert
        assertThrows(AiProviderConfigurationException.class, () -> 
            lessonSummarizationService.summarizeLesson(lessonId, authHeader)
        );
    }
}
