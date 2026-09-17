package com.mentify.ai.service.impl;
 
import com.mentify.ai.config.AiProviderProperties;
import com.mentify.ai.dto.request.AiGenerateRequest;
import com.mentify.ai.dto.response.AiGenerateResponse;
import com.mentify.ai.exception.AiProviderConfigurationException;
import com.mentify.ai.provider.AiProvider;
import com.mentify.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
 
import java.util.HashMap;
import java.util.List;
import java.util.Map;
 
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {
 
    private final AiProviderProperties aiProviderProperties;
    private final List<AiProvider> aiProviders;
 
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
        log.info("Generating AI content using provider: {}", provider.getProviderName());
        return provider.generate(request);
    }
 
    private AiProvider getProvider() {
        String providerName = aiProviderProperties.getProvider().getName();
        return aiProviders.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(providerName))
                .findFirst()
                .orElseThrow(() -> new AiProviderConfigurationException("Unsupported or unconfigured AI provider: " + providerName));
    }
}
