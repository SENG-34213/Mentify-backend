package com.mentify.ai.service.impl;

import com.mentify.ai.client.LessonServiceClient;
import com.mentify.ai.config.AiProviderProperties;
import com.mentify.ai.dto.internal.AiExecutionContext;
import com.mentify.ai.dto.internal.AiExecutionRequest;
import com.mentify.ai.dto.response.AiGenerateResponse;
import com.mentify.ai.dto.response.InternalLessonResponse;
import com.mentify.ai.dto.response.LessonSummaryResponse;
import com.mentify.ai.enums.AiFeatureType;
import com.mentify.ai.enums.AiResponseFormat;
import com.mentify.ai.exception.AiEmptyResponseException;
import com.mentify.ai.exception.AiProviderConfigurationException;
import com.mentify.ai.provider.AiProvider;
import com.mentify.ai.security.AuthenticatedUserService;
import com.mentify.ai.service.LessonSummarizationService;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonSummarizationServiceImpl implements LessonSummarizationService {

    private final LessonServiceClient lessonServiceClient;
    private final List<AiProvider> aiProviders;
    private final AiProviderProperties aiProviderProperties;
    private final AuthenticatedUserService authenticatedUserService;

    private static final String SUMMARIZATION_SYSTEM_PROMPT =
            "You summarize lesson content for a learning management system. " +
            "Provide a concise, structured summary focused on main concepts, key points, and learning objectives.";

    private static final String SUMMARIZATION_USER_INPUT_TEMPLATE =
            "Title: %s\n\nContent:\n%s";

    @Override
    public LessonSummaryResponse summarizeLesson(UUID lessonId, String authorizationHeader) {
        log.info("Starting summarization for lesson ID: {}", lessonId);

        // 1. Fetch lesson content from course-service
        ApiResponse<InternalLessonResponse> lessonApiResponse = lessonServiceClient.getLessonForAi(lessonId, authorizationHeader);
        
        if (lessonApiResponse == null || lessonApiResponse.getData() == null) {
            log.error("Failed to retrieve lesson data for ID: {}", lessonId);
            throw new RuntimeException("Failed to retrieve lesson content");
        }

        InternalLessonResponse lessonData = lessonApiResponse.getData();
        String title = lessonData.getTitle();
        String content = lessonData.getDescription();

        // 2. Validate content
        if (content == null || content.isBlank()) {
            log.warn("Lesson ID: {} has no usable text content", lessonId);
            throw new AiEmptyResponseException("Lesson has no usable text content for summarization");
        }

        // 3. Build internal AI execution request
        String traceId = UUID.randomUUID().toString();
        AiExecutionRequest generateRequest = AiExecutionRequest.builder()
                .featureType(AiFeatureType.LESSON_SUMMARIZATION)
                .userId(authenticatedUserService.getCurrentUserId())
                .context(AiExecutionContext.builder()
                        .courseId(lessonData.getCourseId())
                        .lessonId(lessonId)
                        .build())
                .systemPrompt(SUMMARIZATION_SYSTEM_PROMPT)
                .userInput(String.format(SUMMARIZATION_USER_INPUT_TEMPLATE, title, content))
                .responseFormat(AiResponseFormat.TEXT)
                .traceId(traceId)
                .build();

        // 4. Call AI Provider
        AiProvider provider = getProvider();
        log.info("Summarizing lesson using provider: {} traceId={}", provider.getProviderName(), traceId);
        AiGenerateResponse generateResponse = provider.generate(generateRequest);

        // 5. Return structured response
        return LessonSummaryResponse.builder()
                .lessonId(lessonId)
                .summary(generateResponse.getContent())
                .provider(generateResponse.getProvider())
                .model(generateResponse.getModel())
                .generatedAt(generateResponse.getGeneratedAt())
                .inputTokens(generateResponse.getInputTokens())
                .outputTokens(generateResponse.getOutputTokens())
                .totalTokens(generateResponse.getTotalTokens())
                .estimatedCost(generateResponse.getEstimatedCost())
                .latencyMs(generateResponse.getLatencyMs())
                .finishReason(generateResponse.getFinishReason())
                .providerRequestId(generateResponse.getProviderRequestId())
                .build();
    }

    private AiProvider getProvider() {
        String providerName = aiProviderProperties.getProvider().getName();
        return aiProviders.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(providerName))
                .findFirst()
                .orElseThrow(() -> new AiProviderConfigurationException("Unsupported or unconfigured AI provider: " + providerName));
    }
}
