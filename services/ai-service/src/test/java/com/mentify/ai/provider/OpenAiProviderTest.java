package com.mentify.ai.provider;
 
import com.mentify.ai.config.AiProviderProperties;
import com.mentify.ai.dto.request.AiGenerateRequest;
import com.mentify.ai.dto.response.AiGenerateResponse;
import com.mentify.ai.exception.AiEmptyResponseException;
import com.mentify.ai.exception.AiProviderConfigurationException;
import com.mentify.ai.exception.AiProviderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
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
 
    private OpenAiProvider openAiProvider;
 
    @BeforeEach
    void setUp() {
        properties = new AiProviderProperties();
        openAiProvider = new OpenAiProvider(properties, restClient);
    }
 
    @Test
    void generate_ShouldReturnResponse_WhenOpenAiReturnsValidData() {
        // Arrange
        AiGenerateRequest request = new AiGenerateRequest("Test prompt");
        properties.getOpenai().setApiKey("test-key");
        properties.getOpenai().setModel("gpt-4o");

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
                "model", "gpt-4o",
                "choices", List.of(
                        Map.of("message", Map.of("content", "AI generated content"))
                )
        );
        when(responseSpec.body(Map.class)).thenReturn(openAiResponse);

        // Act
        AiGenerateResponse response = openAiProvider.generate(request);

        // Assert
        assertNotNull(response);
        assertEquals("AI generated content", response.getContent());
        assertEquals("OPENAI", response.getProvider());
        assertEquals("gpt-4o", response.getModel());
    }

    @Test
    void generate_ShouldThrowException_WhenApiKeyIsMissing() {
        // Arrange
        AiGenerateRequest request = new AiGenerateRequest("Test prompt");
        properties.getOpenai().setApiKey(null);
        properties.getProvider().setApiKey(null);

        // Act & Assert
        assertThrows(AiProviderConfigurationException.class, () -> openAiProvider.generate(request));
    }

    @Test
    void generate_ShouldThrowException_WhenResponseIsEmpty() {
        // Arrange
        AiGenerateRequest request = new AiGenerateRequest("Test prompt");
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

        when(responseSpec.body(Map.class)).thenReturn(Map.of()); // Empty map

        // Act & Assert
        assertThrows(AiEmptyResponseException.class, () -> openAiProvider.generate(request));
    }
}
