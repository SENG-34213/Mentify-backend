package com.mentify.ai.provider;
 
import com.mentify.ai.dto.request.AiGenerateRequest;
import com.mentify.ai.dto.response.AiGenerateResponse;
 
/**
 * Interface for AI providers (OpenAI, Gemini, Claude, etc.)
 */
public interface AiProvider {
    /**
     * Get the provider name
     */
    String getProviderName();
 
    /**
     * Check if the provider is available
     */
    boolean isAvailable();
 
    /**
     * Generate content based on a request
     */
    AiGenerateResponse generate(AiGenerateRequest request);
}
