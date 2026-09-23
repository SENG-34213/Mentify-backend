package com.mentify.ai.provider;
 
import com.mentify.ai.config.AiProviderProperties;
import com.mentify.ai.dto.internal.AiExecutionRequest;
import com.mentify.ai.dto.response.AiGenerateResponse;
import com.mentify.ai.enums.AiResponseFormat;
import com.mentify.ai.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
 
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
 
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiProvider implements AiProvider {
 
    private final AiProviderProperties properties;
    private final RestClient restClient;
 
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
 
    @Override
    public String getProviderName() {
        return "OPENAI";
    }
 
    @Override
    public boolean isAvailable() {
        String apiKey = getApiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public AiGenerateResponse generate(AiExecutionRequest request) {
        validateConfig();

        String model = resolveModel(request);
        log.info("Sending request to OpenAI using model: {} feature={} traceId={}",
                model, request.getFeatureType(), request.getTraceId());

        try {
            Map<String, Object> body = buildRequestBody(request, model);
            long startedAt = System.nanoTime();

            ResponseEntity<Map> responseEntity = restClient.post()
                    .uri(OPENAI_URL)
                    .header("Authorization", "Bearer " + getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        log.error("OpenAI client error: {} {}", resp.getStatusCode(), resp.getStatusText());
                        throw new AiProviderException("OpenAI client error: " + resp.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        log.error("OpenAI server error: {} {}", resp.getStatusCode(), resp.getStatusText());
                        throw new AiProviderUnavailableException("OpenAI service unavailable");
                    })
                    .toEntity(Map.class);

            long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
            Map<String, Object> response = responseEntity.getBody();
            String providerRequestId = resolveProviderRequestId(responseEntity, response);
            return mapToResponse(response, latencyMs, providerRequestId);

        } catch (Exception e) {
            if (e instanceof AiProviderException) {
                throw (AiProviderException) e;
            }
            log.error("Unexpected error calling OpenAI", e);
            throw new AiProviderException("Error communicating with AI provider", e);
        }
    }

    private void validateConfig() {
        if (getApiKey() == null || getApiKey().isBlank()) {
            throw new AiProviderConfigurationException("OpenAI API key is missing");
        }
        if (getModel() == null || getModel().isBlank()) {
            throw new AiProviderConfigurationException("OpenAI model is missing");
        }
    }

    private String getApiKey() {
        if (properties.getOpenai().getApiKey() != null && !properties.getOpenai().getApiKey().isBlank()) {
            return properties.getOpenai().getApiKey();
        }
        return properties.getProvider().getApiKey();
    }

    private String getModel() {
        if (properties.getOpenai().getModel() != null && !properties.getOpenai().getModel().isBlank()) {
            return properties.getOpenai().getModel();
        }
        return properties.getProvider().getModel();
    }

    private String resolveModel(AiExecutionRequest request) {
        if (request.getModel() != null && !request.getModel().isBlank()) {
            return request.getModel();
        }
        return getModel();
    }

    private Map<String, Object> buildRequestBody(AiExecutionRequest request, String model) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            messages.add(Map.of("role", "system", "content", request.getSystemPrompt()));
        }
        messages.add(Map.of("role", "user", "content", request.getUserInput()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        if (AiResponseFormat.JSON_OBJECT.equals(request.getResponseFormat())) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        return body;
    }
 
    @SuppressWarnings("unchecked")
    private AiGenerateResponse mapToResponse(Map<String, Object> response, long latencyMs, String providerRequestId) {
        if (response == null || !response.containsKey("choices")) {
            throw new AiEmptyResponseException("Empty or invalid response from OpenAI");
        }
 
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new AiEmptyResponseException("No choices returned from OpenAI");
        }
 
        Map<String, Object> choice = choices.get(0);
        Map<String, Object> message = (Map<String, Object>) choice.get("message");
        String content = (String) message.get("content");
        String finishReason = AiUsageSupport.stringValue(choice, "finish_reason");
        Map<String, Object> usage = (Map<String, Object>) response.getOrDefault("usage", Collections.emptyMap());
        Integer inputTokens = AiUsageSupport.intValue(usage, "prompt_tokens");
        Integer outputTokens = AiUsageSupport.intValue(usage, "completion_tokens");
        Integer totalTokens = AiUsageSupport.intValue(usage, "total_tokens");
 
        if (content == null || content.isBlank()) {
            throw new AiEmptyResponseException("Blank content returned from OpenAI");
        }
 
        return AiGenerateResponse.builder()
                .content(content)
                .provider(getProviderName())
                .model((String) response.get("model"))
                .generatedAt(LocalDateTime.now())
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .totalTokens(totalTokens)
                .estimatedCost(AiUsageSupport.estimateCost(inputTokens, outputTokens, properties.getOpenai().getCost()))
                .latencyMs(latencyMs)
                .finishReason(finishReason)
                .providerRequestId(providerRequestId)
                .build();
    }

    private String resolveProviderRequestId(ResponseEntity<Map> responseEntity, Map<String, Object> response) {
        String headerRequestId = AiUsageSupport.firstHeader(responseEntity.getHeaders(), "x-request-id", "openai-request-id");
        if (headerRequestId != null) {
            return headerRequestId;
        }
        return AiUsageSupport.stringValue(response, "id");
    }
}
