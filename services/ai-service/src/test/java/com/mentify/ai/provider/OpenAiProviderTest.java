package com.mentify.ai.provider;
 
import com.mentify.ai.config.AiProviderProperties;
import com.mentify.ai.dto.internal.AiExecutionRequest;
import com.mentify.ai.dto.response.AiGenerateResponse;
import com.mentify.ai.enums.AiFeatureType;
import com.mentify.ai.enums.AiResponseFormat;
import com.mentify.ai.exception.AiEmptyResponseException;
import com.mentify.ai.exception.AiProviderConfigurationException;
import com.mentify.ai.exception.AiProviderException;
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
class OpenAiProviderTest {
 
    private AiProviderProperties properties;
 
    @Mock
    private RestClient restClient;
 
    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
 
    @Mock
    private RestClient.RequestBodySpec requestBodySpec;
 
    @Mock
    private RestClient.ResponseSpec responseSpec;

    private AiProviderRetryExecutor retryExecutor;

    private AiProviderCircuitBreaker circuitBreaker;
	 
    private OpenAiProvider openAiProvider;
	 
    @BeforeEach
    void setUp() {
        properties = new AiProviderProperties();
        retryExecutor = new AiProviderRetryExecutor(properties);
        circuitBreaker = new AiProviderCircuitBreaker(properties);
        openAiProvider = new OpenAiProvider(properties, restClient, retryExecutor, circuitBreaker);
    }
 
    @Test
    void generate_ShouldReturnResponse_WhenOpenAiReturnsValidData() {
        // Arrange
        AiExecutionRequest request = testRequest("Test prompt");
        properties.getOpenai().setApiKey("test-key");
        properties.getOpenai().setModel("gpt-4o");
        properties.getOpenai().getCost().setInputTokenCostPerMillion(BigDecimal.valueOf(5));
        properties.getOpenai().getCost().setOutputTokenCostPerMillion(BigDecimal.valueOf(15));

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        // Add one more for the second onStatus call in the provider
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        Map<String, Object> openAiResponse = Map.of(
                "id", "chatcmpl-test",
                "model", "gpt-4o",
                "usage", Map.of(
                        "prompt_tokens", 1000,
                        "completion_tokens", 500,
                        "total_tokens", 1500
                ),
                "choices", List.of(
                        Map.of(
                                "finish_reason", "stop",
                                "message", Map.of("content", "AI generated content")
                        )
                )
        );
        when(responseSpec.toEntity(Map.class)).thenReturn(ResponseEntity.ok()
                .header("x-request-id", "req-openai-123")
                .body(openAiResponse));

        // Act
        AiGenerateResponse response = openAiProvider.generate(request);

        // Assert
        assertNotNull(response);
        assertEquals("AI generated content", response.getContent());
        assertEquals("OPENAI", response.getProvider());
        assertEquals("gpt-4o", response.getModel());
        assertEquals(1000, response.getInputTokens());
        assertEquals(500, response.getOutputTokens());
        assertEquals(1500, response.getTotalTokens());
        assertEquals(new BigDecimal("0.01250000"), response.getEstimatedCost());
        assertNotNull(response.getLatencyMs());
        assertEquals("stop", response.getFinishReason());
        assertEquals("req-openai-123", response.getProviderRequestId());
    }

    @Test
    void generate_ShouldThrowException_WhenApiKeyIsMissing() {
        // Arrange
        AiExecutionRequest request = testRequest("Test prompt");
        properties.getOpenai().setApiKey(null);
        properties.getProvider().setApiKey(null);

        // Act & Assert
        assertThrows(AiProviderConfigurationException.class, () -> openAiProvider.generate(request));
    }

    @Test
    void generate_ShouldThrowException_WhenResponseIsEmpty() {
        // Arrange
        AiExecutionRequest request = testRequest("Test prompt");
        properties.getOpenai().setApiKey("test-key");
        properties.getOpenai().setModel("gpt-4o");

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        when(responseSpec.toEntity(Map.class)).thenReturn(ResponseEntity.ok(Map.of())); // Empty map

        // Act & Assert
        assertThrows(AiEmptyResponseException.class, () -> openAiProvider.generate(request));
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
