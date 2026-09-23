package com.mentify.ai.provider;

import com.mentify.ai.config.AiProviderProperties;
import com.mentify.ai.dto.internal.AiExecutionRequest;
import com.mentify.ai.dto.response.AiGenerateResponse;
import com.mentify.ai.enums.AiFeatureType;
import com.mentify.ai.enums.AiResponseFormat;
import com.mentify.ai.exception.AiEmptyResponseException;
import com.mentify.ai.exception.AiProviderConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiAiProviderTest {

    private AiProviderProperties properties;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private GeminiAiProvider geminiAiProvider;

    @BeforeEach
    void setUp() {
        properties = new AiProviderProperties();
        geminiAiProvider = new GeminiAiProvider(properties, restClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void generate_ShouldReturnResponse_WhenGeminiReturnsValidData() {
        // Arrange
        AiExecutionRequest request = testRequest("Test prompt");
        properties.getGemini().setApiKey("test-key");
        properties.getGemini().setModel("gemini-3-flash-preview");
        properties.getGemini().getCost().setInputTokenCostPerMillion(BigDecimal.valueOf(1));
        properties.getGemini().getCost().setOutputTokenCostPerMillion(BigDecimal.valueOf(2));

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        Map<String, Object> geminiResponse = Map.of(
                "responseId", "gemini-response-123",
                "usageMetadata", Map.of(
                        "promptTokenCount", 1000,
                        "candidatesTokenCount", 2000,
                        "totalTokenCount", 3000
                ),
                "candidates", List.of(
                        Map.of(
                                "finishReason", "STOP",
                                "content", Map.of(
                                        "parts", List.of(
                                                Map.of("text", "AI generated content")
                                        )
                                )
                        )
                )
        );
        when(responseSpec.toEntity(Map.class)).thenReturn(ResponseEntity.ok()
                .header("x-goog-request-id", "req-gemini-123")
                .body(geminiResponse));

        // Act
        AiGenerateResponse response = geminiAiProvider.generate(request);

        // Assert
        assertNotNull(response);
        assertEquals("AI generated content", response.getContent());
        assertEquals("GEMINI", response.getProvider());
        assertEquals("gemini-3-flash-preview", response.getModel());
        assertEquals(1000, response.getInputTokens());
        assertEquals(2000, response.getOutputTokens());
        assertEquals(3000, response.getTotalTokens());
        assertEquals(new BigDecimal("0.00500000"), response.getEstimatedCost());
        assertNotNull(response.getLatencyMs());
        assertEquals("STOP", response.getFinishReason());
        assertEquals("req-gemini-123", response.getProviderRequestId());
    }

    @Test
    void generate_ShouldThrowException_WhenApiKeyIsMissing() {
        // Arrange
        AiExecutionRequest request = testRequest("Test prompt");
        properties.getGemini().setApiKey(null);
        properties.getProvider().setApiKey(null);

        // Act & Assert
        assertThrows(AiProviderConfigurationException.class, () -> geminiAiProvider.generate(request));
    }

    @Test
    @SuppressWarnings("unchecked")
    void generate_ShouldThrowException_WhenResponseIsEmpty() {
        // Arrange
        AiExecutionRequest request = testRequest("Test prompt");
        properties.getGemini().setApiKey("test-key");
        properties.getGemini().setModel("gemini-3-flash-preview");

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        when(responseSpec.toEntity(Map.class)).thenReturn(ResponseEntity.ok(Map.of())); // Empty map

        // Act & Assert
        assertThrows(AiEmptyResponseException.class, () -> geminiAiProvider.generate(request));
    }

    private AiExecutionRequest testRequest(String userInput) {
        return AiExecutionRequest.builder()
                .featureType(AiFeatureType.GENERAL_GENERATION)
                .systemPrompt("Test system prompt")
                .userInput(userInput)
                .responseFormat(AiResponseFormat.TEXT)
                .traceId("test-trace-id")
                .build();
    }
}
