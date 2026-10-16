package com.mentify.ai.service;

import com.mentify.ai.dto.response.AiGenerateResponse;
import com.mentify.ai.enums.AiFeatureType;

import java.util.UUID;

public interface AiAuditService {
    void recordAllowed(String traceId, AiFeatureType featureType, UUID userId, UUID courseId, UUID lessonId);

    void recordBlocked(String traceId, AiFeatureType featureType, UUID userId, UUID courseId, UUID lessonId, String reason);

    void recordFailed(String traceId, AiFeatureType featureType, UUID userId, UUID courseId, UUID lessonId, String reason);

    void recordCompleted(String traceId, AiFeatureType featureType, UUID userId, UUID courseId, UUID lessonId, AiGenerateResponse response);
}
