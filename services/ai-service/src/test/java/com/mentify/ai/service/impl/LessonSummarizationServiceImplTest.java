package com.mentify.ai.service.impl;

import com.mentify.ai.client.LessonServiceClient;
import com.mentify.ai.config.AiProviderProperties;
import com.mentify.ai.dto.request.AiGenerateRequest;
import com.mentify.ai.dto.response.AiGenerateResponse;
import com.mentify.ai.dto.response.InternalLessonResponse;
import com.mentify.ai.dto.response.LessonSummaryResponse;
import com.mentify.ai.exception.AiEmptyResponseException;
import com.mentify.ai.exception.AiProviderConfigurationException;
import com.mentify.ai.provider.AiProvider;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonSummarizationServiceImplTest {

    @Mock
    private LessonServiceClient lessonServiceClient;

    @Mock
    private AiProvider aiProvider;

    private AiProviderProperties properties;

    private LessonSummarizationServiceImpl lessonSummarizationService;

    @BeforeEach
    void setUp() {
        properties = new AiProviderProperties();
        properties.getProvider().setName("GEMINI");
        
        lessonSummarizationService = new LessonSummarizationServiceImpl(
                lessonServiceClient,
                List.of(aiProvider),
                properties
        );
    }

    @Test
    void summarizeLesson_ShouldReturnSummary_WhenValidLessonProvided() {
        // Arrange
        when(aiProvider.getProviderName()).thenReturn("GEMINI");
        UUID lessonId = UUID.randomUUID();
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
        
        AiGenerateResponse generateResponse = AiGenerateResponse.builder()
                .content("This is a summary")
                .provider("GEMINI")
                .model("gemini-3-flash-preview")
                .generatedAt(LocalDateTime.now())
                .build();
        
        when(aiProvider.generate(any(AiGenerateRequest.class))).thenReturn(generateResponse);

        // Act
        LessonSummaryResponse response = lessonSummarizationService.summarizeLesson(lessonId, authHeader);

        // Assert
        assertNotNull(response);
        assertEquals(lessonId, response.getLessonId());
        assertEquals("This is a summary", response.getSummary());
        assertEquals("GEMINI", response.getProvider());
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

        // Act & Assert
        assertThrows(AiProviderConfigurationException.class, () -> 
            lessonSummarizationService.summarizeLesson(lessonId, authHeader)
        );
    }
}
