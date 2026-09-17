package com.mentify.ai.provider;

import com.mentify.ai.config.AiProviderProperties;
import com.mentify.ai.dto.request.AiGenerateRequest;
import com.mentify.ai.dto.response.AiGenerateResponse;
import com.mentify.ai.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiAiProvider implements AiProvider {

    private final AiProviderProperties properties;
    private final RestClient restClient;

    private static final String GEMINI_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    @Override
    public String getProviderName() {
        return "GEMINI";
    }

    @Override
    public boolean isAvailable() {
        String apiKey = getApiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public AiGenerateResponse generate(AiGenerateRequest request) {
        validateConfig();

        String model = getModel();
        String apiKey = getApiKey();
        String url = String.format(GEMINI_URL_TEMPLATE, model, apiKey);

        log.info("Sending request to Gemini using model: {}", model);

        try {
            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", request.getPrompt())
                            ))
                    )
            );

            Map<String, Object> response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        log.error("Gemini client error: {} {}", resp.getStatusCode(), resp.getStatusText());
                        throw new AiProviderException("Gemini client error: " + resp.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        log.error("Gemini server error: {} {}", resp.getStatusCode(), resp.getStatusText());
                        throw new AiProviderUnavailableException("Gemini service unavailable");
                    })
                    .body(Map.class);

            return mapToResponse(response, model);

        } catch (Exception e) {
            if (e instanceof AiProviderException) {
                throw (AiProviderException) e;
            }
            log.error("Unexpected error calling Gemini", e);
            throw new AiProviderException("Error communicating with AI provider", e);
        }
    }

    private void validateConfig() {
        if (getApiKey() == null || getApiKey().isBlank()) {
            throw new AiProviderConfigurationException("Gemini API key is missing");
        }
        if (getModel() == null || getModel().isBlank()) {
            throw new AiProviderConfigurationException("Gemini model is missing");
        }
    }

    private String getApiKey() {
        if (properties.getGemini().getApiKey() != null && !properties.getGemini().getApiKey().isBlank()) {
            return properties.getGemini().getApiKey();
        }
        return properties.getProvider().getApiKey();
    }

    private String getModel() {
        if (properties.getGemini().getModel() != null && !properties.getGemini().getModel().isBlank()) {
            return properties.getGemini().getModel();
        }
        return properties.getProvider().getModel();
    }

    @SuppressWarnings("unchecked")
    private AiGenerateResponse mapToResponse(Map<String, Object> response, String model) {
        if (response == null || !response.containsKey("candidates")) {
            throw new AiEmptyResponseException("Empty or invalid response from Gemini");
        }

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new AiEmptyResponseException("No candidates returned from Gemini");
        }

        Map<String, Object> candidate = candidates.get(0);
        Map<String, Object> contentMap = (Map<String, Object>) candidate.get("content");
        if (contentMap == null || !contentMap.containsKey("parts")) {
            throw new AiEmptyResponseException("No content or parts returned from Gemini");
        }

        List<Map<String, Object>> parts = (List<Map<String, Object>>) contentMap.get("parts");
        if (parts == null || parts.isEmpty()) {
            throw new AiEmptyResponseException("No parts returned from Gemini");
        }

        String text = (String) parts.get(0).get("text");

        if (text == null || text.isBlank()) {
            throw new AiEmptyResponseException("Blank content returned from Gemini");
        }

        return AiGenerateResponse.builder()
                .content(text)
                .provider(getProviderName())
                .model(model)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}