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

    private static final String SUMMARIZATION_PROMPT_TEMPLATE = 
            "Please provide a concise and structured summary of the following lesson content. " +
            "Focus on the main concepts, key points, and learning objectives.\n\n" +
            "Title: %s\n\n" +
            "Content:\n%s";

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

        // 3. Build prompt
        String prompt = String.format(SUMMARIZATION_PROMPT_TEMPLATE, title, content);

        // 4. Call AI Provider
        AiGenerateRequest generateRequest = AiGenerateRequest.builder()
                .prompt(prompt)
                .build();

        AiProvider provider = getProvider();
        AiGenerateResponse generateResponse = provider.generate(generateRequest);

        // 5. Return structured response
        return LessonSummaryResponse.builder()
                .lessonId(lessonId)
                .summary(generateResponse.getContent())
                .provider(generateResponse.getProvider())
                .model(generateResponse.getModel())
                .generatedAt(generateResponse.getGeneratedAt())
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
