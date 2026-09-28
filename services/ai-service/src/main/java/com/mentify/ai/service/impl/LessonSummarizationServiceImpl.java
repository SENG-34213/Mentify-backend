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
import com.mentify.ai.exception.AiContentPolicyException;
import com.mentify.ai.exception.AiEmptyResponseException;
import com.mentify.ai.exception.AiProviderConfigurationException;
import com.mentify.ai.exception.AiQuotaExceededException;
import com.mentify.ai.prompt.AiPromptRegistry;
import com.mentify.ai.prompt.AiPromptTemplate;
import com.mentify.ai.provider.AiProvider;
import com.mentify.ai.security.AuthenticatedUserService;
import com.mentify.ai.service.AiAuditService;
import com.mentify.ai.service.AiContentGuardService;
import com.mentify.ai.service.AiUsageGuardService;
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
    private final AiPromptRegistry promptRegistry;
    private final AiUsageGuardService usageGuardService;
    private final AiContentGuardService contentGuardService;
    private final AiAuditService auditService;

    @Override
    public LessonSummaryResponse summarizeLesson(UUID lessonId, String authorizationHeader) {
        log.info("Starting summarization for lesson ID: {}", lessonId);
        String traceId = UUID.randomUUID().toString();
        UUID userId = authenticatedUserService.getCurrentUserId();
        AiFeatureType featureType = AiFeatureType.LESSON_SUMMARIZATION;
        UUID courseId = null;

        try {
            usageGuardService.assertAllowed(userId, featureType);

            // 1. Fetch lesson content from course-service. The course-service internal endpoint
            // validates ownership/enrollment from the forwarded Authorization token.
            ApiResponse<InternalLessonResponse> lessonApiResponse = lessonServiceClient.getLessonForAi(lessonId, authorizationHeader);

            if (lessonApiResponse == null || lessonApiResponse.getData() == null) {
                log.error("Failed to retrieve lesson data for ID: {}", lessonId);
                throw new RuntimeException("Failed to retrieve lesson content");
            }

            InternalLessonResponse lessonData = lessonApiResponse.getData();
            if (!lessonId.equals(lessonData.getId())) {
                throw new AiProviderConfigurationException("Course service returned mismatched lesson data");
            }

            courseId = lessonData.getCourseId();
            String title = lessonData.getTitle();
            String content = lessonData.getDescription();

            // 2. Validate content
            if (content == null || content.isBlank()) {
                log.warn("Lesson ID: {} has no usable text content", lessonId);
                throw new AiEmptyResponseException("Lesson has no usable text content for summarization");
            }

            String guardedTitle = contentGuardService.sanitizeForPrompt(featureType, "lesson_title", title);
            String guardedContent = contentGuardService.sanitizeForPrompt(featureType, "lesson_content", content);
            auditService.recordAllowed(traceId, featureType, userId, courseId, lessonId);

            // 3. Build internal AI execution request
            AiPromptTemplate prompt = promptRegistry.get(AiPromptRegistry.LESSON_SUMMARY, AiPromptRegistry.V1);
            AiExecutionRequest generateRequest = AiExecutionRequest.builder()
                    .featureType(featureType)
                    .userId(userId)
                    .context(AiExecutionContext.builder()
                            .courseId(courseId)
                            .lessonId(lessonId)
                            .build())
                    .systemPrompt(prompt.systemPrompt())
                    .userInput(prompt.renderUserInput(guardedTitle, guardedContent))
                    .responseFormat(AiResponseFormat.TEXT)
                    .traceId(traceId)
                    .build();

            // 4. Call AI Provider
            AiProvider provider = getProvider();
            log.info("Summarizing lesson using provider: {} prompt={} traceId={}",
                    provider.getProviderName(), prompt.promptId(), traceId);
            AiGenerateResponse generateResponse = provider.generate(generateRequest);
            auditService.recordCompleted(traceId, featureType, userId, courseId, lessonId, generateResponse);

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
        } catch (RuntimeException ex) {
            if (isGuardrailBlock(ex)) {
                auditService.recordBlocked(traceId, featureType, userId, courseId, lessonId, ex.getClass().getSimpleName());
            } else {
                auditService.recordFailed(traceId, featureType, userId, courseId, lessonId, ex.getClass().getSimpleName());
            }
            throw ex;
        }
    }

    private boolean isGuardrailBlock(RuntimeException ex) {
        return ex instanceof AiContentPolicyException || ex instanceof AiQuotaExceededException;
    }

    private AiProvider getProvider() {
        String providerName = aiProviderProperties.getProvider().getName();
        return aiProviders.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(providerName))
                .findFirst()
                .orElseThrow(() -> new AiProviderConfigurationException("Unsupported or unconfigured AI provider: " + providerName));
    }
}
