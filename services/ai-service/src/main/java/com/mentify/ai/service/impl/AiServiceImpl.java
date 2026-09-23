package com.mentify.ai.service.impl;
 
import com.mentify.ai.config.AiProviderProperties;
import com.mentify.ai.dto.internal.AiExecutionRequest;
import com.mentify.ai.dto.request.AiGenerateRequest;
import com.mentify.ai.dto.response.AiGenerateResponse;
import com.mentify.ai.enums.AiFeatureType;
import com.mentify.ai.enums.AiResponseFormat;
import com.mentify.ai.exception.AiProviderConfigurationException;
import com.mentify.ai.provider.AiProvider;
import com.mentify.ai.security.AuthenticatedUserService;
import com.mentify.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
 
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
 
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {
 
    private final AiProviderProperties aiProviderProperties;
    private final List<AiProvider> aiProviders;
    private final AuthenticatedUserService authenticatedUserService;
 
    @Override
    public Map<String, String> getServiceStatus() {
        Map<String, String> status = new HashMap<>();
        status.put("service", "ai-service");
        status.put("status", "READY");
        status.put("configuredProvider", aiProviderProperties.getProvider().getName());
        status.put("configuredModel", aiProviderProperties.getProvider().getModel());
        return status;
    }
 
    @Override
    public AiGenerateResponse generate(AiGenerateRequest request) {
        AiProvider provider = getProvider();
        String traceId = UUID.randomUUID().toString();
        AiExecutionRequest executionRequest = AiExecutionRequest.builder()
                .featureType(AiFeatureType.GENERAL_GENERATION)
                .userId(authenticatedUserService.getCurrentUserId())
                .systemPrompt("You are Mentify's AI assistant. Provide accurate, concise educational support.")
                .userInput(request.getPrompt())
                .responseFormat(AiResponseFormat.TEXT)
                .traceId(traceId)
                .build();

        log.info("Generating AI content using provider: {} traceId={}", provider.getProviderName(), traceId);
        return provider.generate(executionRequest);
    }
 
    private AiProvider getProvider() {
        String providerName = aiProviderProperties.getProvider().getName();
        return aiProviders.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(providerName))
                .findFirst()
                .orElseThrow(() -> new AiProviderConfigurationException("Unsupported or unconfigured AI provider: " + providerName));
    }
}
