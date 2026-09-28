package com.mentify.ai.service.impl;

import com.mentify.ai.config.AiProviderProperties;
import com.mentify.ai.enums.AiFeatureType;
import com.mentify.ai.exception.AiContentPolicyException;
import com.mentify.ai.service.AiContentGuardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AiContentGuardServiceImpl implements AiContentGuardService {
    private static final List<Pattern> PROMPT_INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)\\bignore\\s+(all\\s+)?(previous|prior|above)\\s+(instructions|rules|prompt)\\b"),
            Pattern.compile("(?i)\\b(system|developer)\\s+prompt\\b"),
            Pattern.compile("(?i)\\breveal\\s+(the\\s+)?(prompt|instructions|policy|system message)\\b"),
            Pattern.compile("(?i)\\bdisregard\\s+(previous|prior|above)\\s+(instructions|rules)\\b"),
            Pattern.compile("(?i)\\byou\\s+are\\s+now\\s+(in\\s+)?developer\\s+mode\\b")
    );

    private final AiProviderProperties properties;

    @Override
    public String sanitizeForPrompt(AiFeatureType featureType, String label, String content) {
        if (content == null) {
            return "";
        }

        String sanitized = stripControlCharacters(content).trim();
        int maxInputCharacters = properties.getGuardrails().getMaxInputCharacters();
        if (sanitized.length() > maxInputCharacters) {
            sanitized = sanitized.substring(0, maxInputCharacters);
        }

        enforceContentFilter(label, sanitized);
        enforcePromptInjectionControl(label, sanitized);

        return """
                <untrusted_%s>
                %s
                </untrusted_%s>
                """.formatted(label, sanitized, label).trim();
    }

    private String stripControlCharacters(String content) {
        return content.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", " ");
    }

    private void enforceContentFilter(String label, String content) {
        if (!properties.getGuardrails().isContentFilteringEnabled()) {
            return;
        }

        String lowerContent = content.toLowerCase(Locale.ROOT);
        for (String blockedTerm : properties.getGuardrails().getBlockedTerms()) {
            if (blockedTerm != null && !blockedTerm.isBlank()
                    && lowerContent.contains(blockedTerm.toLowerCase(Locale.ROOT))) {
                throw new AiContentPolicyException("AI input failed content policy for " + label);
            }
        }
    }

    private void enforcePromptInjectionControl(String label, String content) {
        if (!properties.getGuardrails().isPromptInjectionDetectionEnabled()) {
            return;
        }

        for (Pattern pattern : PROMPT_INJECTION_PATTERNS) {
            if (pattern.matcher(content).find()) {
                throw new AiContentPolicyException("Potential prompt injection detected in " + label);
            }
        }
    }
}
