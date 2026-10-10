package com.mentify.ai.service;

import com.mentify.ai.enums.AiFeatureType;

public interface AiContentGuardService {
    String sanitizeForPrompt(AiFeatureType featureType, String label, String content);
}
