package com.mentify.ai.provider;

import com.mentify.ai.config.AiProviderProperties;
import com.mentify.ai.dto.request.AiGenerateRequest;
import com.mentify.ai.dto.response.AiGenerateResponse;
import com.mentify.ai.exception.AiEmptyResponseException;
import com.mentify.ai.exception.AiProviderConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

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
        AiGenerateRequest request = new AiGenerateRequest("Test prompt");
        properties.getGemini().setApiKey("test-key");
        properties.getGemini().setModel("gemini-3-flash-preview");

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        Map<String, Object> geminiResponse = Map.of(
                "candidates", List.of(
                        Map.of("content", Map.of(
                                "parts", List.of(
                                        Map.of("text", "AI generated content")
                                )
                        ))
                )
        );
        when(responseSpec.body(Map.class)).thenReturn(geminiResponse);

        // Act
        AiGenerateResponse response = geminiAiProvider.generate(request);

        // Assert
        assertNotNull(response);
        assertEquals("AI generated content", response.getContent());
        assertEquals("GEMINI", response.getProvider());
        assertEquals("gemini-3-flash-preview", response.getModel());
    }

    @Test
    void generate_ShouldThrowException_WhenApiKeyIsMissing() {
        // Arrange
        AiGenerateRequest request = new AiGenerateRequest("Test prompt");
        properties.getGemini().setApiKey(null);
        properties.getProvider().setApiKey(null);

        // Act & Assert
        assertThrows(AiProviderConfigurationException.class, () -> geminiAiProvider.generate(request));
    }

    @Test
    @SuppressWarnings("unchecked")
    void generate_ShouldThrowException_WhenResponseIsEmpty() {
        // Arrange
        AiGenerateRequest request = new AiGenerateRequest("Test prompt");
        properties.getGemini().setApiKey("test-key");
        properties.getGemini().setModel("gemini-3-flash-preview");

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        when(responseSpec.body(Map.class)).thenReturn(Map.of()); // Empty map

        // Act & Assert
        assertThrows(AiEmptyResponseException.class, () -> geminiAiProvider.generate(request));
    }
}