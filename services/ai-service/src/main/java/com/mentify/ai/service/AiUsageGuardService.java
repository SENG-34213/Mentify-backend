package com.mentify.ai.service;

import com.mentify.ai.enums.AiFeatureType;

import java.util.UUID;

public interface AiUsageGuardService {
    void assertAllowed(UUID userId, AiFeatureType featureType);
}
