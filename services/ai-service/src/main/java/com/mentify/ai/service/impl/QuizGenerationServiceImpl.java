package com.mentify.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.ai.config.AiProviderProperties;
import com.mentify.ai.config.QuizGenerationProperties;
import com.mentify.ai.dto.internal.AiExecutionContext;
import com.mentify.ai.dto.internal.AiExecutionRequest;
import com.mentify.ai.dto.request.QuizGenerationRequest;
import com.mentify.ai.dto.response.AiGenerateResponse;
import com.mentify.ai.dto.response.GeneratedQuestionResponse;
import com.mentify.ai.dto.response.GeneratedQuizDraftResponse;
import com.mentify.ai.enums.AiFeatureType;
import com.mentify.ai.enums.AiResponseFormat;
import com.mentify.ai.exception.AiContentPolicyException;
import com.mentify.ai.exception.AiInvalidGenerationException;
import com.mentify.ai.exception.AiProviderConfigurationException;
import com.mentify.ai.exception.AiQuotaExceededException;
import com.mentify.ai.prompt.QuizGenerationPromptBuilder;
import com.mentify.ai.provider.AiProvider;
import com.mentify.ai.security.AuthenticatedUserService;
import com.mentify.ai.service.AiAuditService;
import com.mentify.ai.service.AiContentGuardService;
import com.mentify.ai.service.AiUsageGuardService;
import com.mentify.ai.service.DocumentContentService;
import com.mentify.ai.service.QuizGenerationService;
import com.mentify.ai.validation.GeneratedQuizValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizGenerationServiceImpl implements QuizGenerationService {

    private static final String SUPPORTED_QUESTION_TYPE = "MULTIPLE_CHOICE_SINGLE_ANSWER";
    private static final UUID PUBLIC_TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final DocumentContentService documentContentService;
    private final List<AiProvider> aiProviders;
    private final AiProviderProperties aiProviderProperties;
    private final QuizGenerationProperties quizGenerationProperties;
    private final AuthenticatedUserService authenticatedUserService;
    private final AiUsageGuardService usageGuardService;
    private final AiContentGuardService contentGuardService;
    private final AiAuditService auditService;
    private final QuizGenerationPromptBuilder promptBuilder;
    private final GeneratedQuizValidator generatedQuizValidator;
    private final ObjectMapper objectMapper;

    @Override
    public GeneratedQuizDraftResponse generateQuiz(QuizGenerationRequest request, MultipartFile file) {
        validateRequest(request);

        String traceId = UUID.randomUUID().toString();
        UUID userId = resolveUserId();
        AiFeatureType featureType = AiFeatureType.QUIZ_GENERATION;

        try {
            usageGuardService.assertAllowed(userId, featureType);
            String documentText = documentContentService.extractReadableText(file, request.getQuestionCount());
            String guardedDocument = contentGuardService.sanitizeForPrompt(featureType, "quiz_document", documentText);
            auditService.recordAllowed(traceId, featureType, userId, request.getCourseId(), null);

            AiProvider provider = getProvider();
            AiGenerateResponse generateResponse = generateValidResponse(provider, request, guardedDocument, traceId, userId);
            List<GeneratedQuestionResponse> questions = parseQuestions(generateResponse.getContent());
            generatedQuizValidator.validate(questions, request.getQuestionCount(), request.getQuestionType());
            auditService.recordCompleted(traceId, featureType, userId, request.getCourseId(), null, generateResponse);

            return GeneratedQuizDraftResponse.builder()
                    .courseId(request.getCourseId())
                    .questionCount(request.getQuestionCount())
                    .difficulty(request.getDifficulty())
                    .questionType(request.getQuestionType())
                    .questions(questions)
                    .provider(generateResponse.getProvider())
                    .model(generateResponse.getModel())
                    .generatedAt(generateResponse.getGeneratedAt())
                    .build();
        } catch (RuntimeException ex) {
            if (isGuardrailBlock(ex)) {
                auditService.recordBlocked(traceId, featureType, userId, request.getCourseId(), null, ex.getClass().getSimpleName());
            } else {
                auditService.recordFailed(traceId, featureType, userId, request.getCourseId(), null, ex.getClass().getSimpleName());
            }
            throw ex;
        }
    }

    private void validateRequest(QuizGenerationRequest request) {
        if (request.getCourseId() == null) {
            throw new AiInvalidGenerationException("Course ID is required");
        }
        if (request.getQuestionCount() == null
                || request.getQuestionCount() < 1
                || request.getQuestionCount() > quizGenerationProperties.getMaxQuestionCount()) {
            throw new AiInvalidGenerationException("Question count must be between 1 and " + quizGenerationProperties.getMaxQuestionCount());
        }
        if (request.getDifficulty() == null || !List.of("EASY", "MEDIUM", "HARD").contains(request.getDifficulty())) {
            throw new AiInvalidGenerationException("Difficulty must be EASY, MEDIUM, or HARD");
        }
        if (!SUPPORTED_QUESTION_TYPE.equals(request.getQuestionType())) {
            throw new AiInvalidGenerationException("Unsupported question type: " + request.getQuestionType());
        }
    }

    private AiGenerateResponse generateValidResponse(
            AiProvider provider,
            QuizGenerationRequest request,
            String guardedDocument,
            String traceId,
            UUID userId
    ) {
        RuntimeException lastFailure = null;
        int attempts = quizGenerationProperties.getRegenerationAttempts() + 1;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            AiGenerateResponse response = provider.generate(buildExecutionRequest(request, guardedDocument, traceId, userId));
            try {
                List<GeneratedQuestionResponse> questions = parseQuestions(response.getContent());
                generatedQuizValidator.validate(questions, request.getQuestionCount(), request.getQuestionType());
                return response;
            } catch (AiInvalidGenerationException ex) {
                lastFailure = ex;
                log.warn("Invalid quiz generation response traceId={} attempt={}", traceId, attempt);
            }
        }
        throw lastFailure == null
                ? new AiInvalidGenerationException("AI response could not be validated")
                : lastFailure;
    }

    private AiExecutionRequest buildExecutionRequest(
            QuizGenerationRequest request,
            String guardedDocument,
            String traceId,
            UUID userId
    ) {
        return AiExecutionRequest.builder()
                .featureType(AiFeatureType.QUIZ_GENERATION)
                .userId(userId)
                .context(AiExecutionContext.builder()
                        .courseId(request.getCourseId())
                        .build())
                .systemPrompt(promptBuilder.systemPrompt())
                .userInput(promptBuilder.userInput(request, guardedDocument))
                .temperature(quizGenerationProperties.getTemperature())
                .maxTokens(quizGenerationProperties.getMaxOutputTokens())
                .responseFormat(AiResponseFormat.JSON_OBJECT)
                .traceId(traceId)
                .build();
    }

    private List<GeneratedQuestionResponse> parseQuestions(String content) {
        Throwable lastFailure = null;
        for (String candidate : jsonCandidates(content)) {
            try {
                JsonNode root = parseJsonNode(candidate);
                JsonNode questionsNode = resolveQuestionsNode(root);
                if (questionsNode == null || !questionsNode.isArray()) {
                    lastFailure = new AiInvalidGenerationException("AI response did not contain a questions array");
                    continue;
                }
                return objectMapper.convertValue(
                        questionsNode,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, GeneratedQuestionResponse.class)
                );
            } catch (JsonProcessingException ex) {
                lastFailure = ex;
            } catch (IllegalArgumentException ex) {
                lastFailure = ex;
            }
        }
        log.warn("AI quiz response was not valid JSON. Preview={}", preview(content), lastFailure);
        throw new AiInvalidGenerationException("AI response was not valid JSON", lastFailure);
    }

    private JsonNode parseJsonNode(String content) throws JsonProcessingException {
        String json = removeTrailingCommas(stripJsonFence(content));
        JsonNode root = objectMapper.readTree(json);
        if (root != null && root.isTextual()) {
            root = objectMapper.readTree(removeTrailingCommas(stripJsonFence(root.asText())));
        }
        return root;
    }

    private JsonNode resolveQuestionsNode(JsonNode root) {
        if (root == null) {
            return null;
        }
        if (root.isArray()) {
            return root;
        }
        return findQuestionsNode(root);
    }

    private JsonNode findQuestionsNode(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        JsonNode directQuestions = node.get("questions");
        if (directQuestions != null) {
            if (directQuestions.isTextual()) {
                try {
                    return objectMapper.readTree(removeTrailingCommas(stripJsonFence(directQuestions.asText())));
                } catch (JsonProcessingException ex) {
                    return directQuestions;
                }
            }
            return directQuestions;
        }
        for (JsonNode child : node) {
            JsonNode nestedQuestions = findQuestionsNode(child);
            if (nestedQuestions != null) {
                return nestedQuestions;
            }
        }
        return null;
    }

    private List<String> jsonCandidates(String content) {
        List<String> candidates = new ArrayList<>();
        if (content == null || content.isBlank()) {
            candidates.add("");
            return candidates;
        }

        String stripped = stripJsonFence(content);
        candidates.add(stripped);

        for (int index = 0; index < stripped.length(); index++) {
            char current = stripped.charAt(index);
            if (current == '{' || current == '[' || current == '"') {
                String candidate = extractJsonValueAt(stripped, index);
                if (candidate != null && !candidates.contains(candidate)) {
                    candidates.add(candidate);
                }
            }
        }
        return candidates;
    }

    private String stripJsonFence(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed;
    }

    private String extractJsonValueAt(String content, int start) {
        char first = content.charAt(start);
        if (first == '"') {
            return extractJsonStringAt(content, start);
        }

        boolean inString = false;
        boolean escaped = false;
        List<Character> stack = new ArrayList<>();
        for (int index = start; index < content.length(); index++) {
            char current = content.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\' && inString) {
                escaped = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (current == '{') {
                stack.add('}');
            } else if (current == '[') {
                stack.add(']');
            } else if (current == '}' || current == ']') {
                if (stack.isEmpty() || stack.get(stack.size() - 1) != current) {
                    return null;
                }
                stack.remove(stack.size() - 1);
                if (stack.isEmpty()) {
                    return removeTrailingCommas(content.substring(start, index + 1));
                }
            }
        }
        return null;
    }

    private String extractJsonStringAt(String content, int start) {
        boolean escaped = false;
        for (int index = start + 1; index < content.length(); index++) {
            char current = content.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                return content.substring(start, index + 1);
            }
        }
        return null;
    }

    private String removeTrailingCommas(String content) {
        StringBuilder result = new StringBuilder(content.length());
        boolean inString = false;
        boolean escaped = false;
        for (int index = 0; index < content.length(); index++) {
            char current = content.charAt(index);
            if (escaped) {
                escaped = false;
                result.append(current);
                continue;
            }
            if (current == '\\' && inString) {
                escaped = true;
                result.append(current);
                continue;
            }
            if (current == '"') {
                inString = !inString;
                result.append(current);
                continue;
            }
            if (!inString && current == ',' && isFollowedByClosingBracket(content, index + 1)) {
                continue;
            }
            result.append(current);
        }
        return result.toString();
    }

    private boolean isFollowedByClosingBracket(String content, int start) {
        for (int index = start; index < content.length(); index++) {
            char current = content.charAt(index);
            if (!Character.isWhitespace(current)) {
                return current == '}' || current == ']';
            }
        }
        return false;
    }

    private String preview(String content) {
        if (content == null) {
            return "<null>";
        }
        String compact = content.replaceAll("\\s+", " ").trim();
        return compact.length() <= 300 ? compact : compact.substring(0, 300) + "...";
    }

    private boolean isGuardrailBlock(RuntimeException ex) {
        return ex instanceof AiContentPolicyException || ex instanceof AiQuotaExceededException;
    }

    private UUID resolveUserId() {
        try {
            return authenticatedUserService.getCurrentUserId();
        } catch (RuntimeException ex) {
            return PUBLIC_TEST_USER_ID;
        }
    }

    private AiProvider getProvider() {
        String providerName = aiProviderProperties.getProvider().getName();
        return aiProviders.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(providerName))
                .findFirst()
                .orElseThrow(() -> new AiProviderConfigurationException("Unsupported or unconfigured AI provider: " + providerName));
    }

}
