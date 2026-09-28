package com.mentify.ai.service.impl;

import com.mentify.ai.dto.response.AiGenerateResponse;
import com.mentify.ai.entity.AiAuditEvent;
import com.mentify.ai.enums.AiFeatureType;
import com.mentify.ai.repository.AiAuditEventRepository;
import com.mentify.ai.service.AiAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAuditServiceImpl implements AiAuditService {
    private static final String STATUS_ALLOWED = "ALLOWED";
    private static final String STATUS_BLOCKED = "BLOCKED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";

    private final AiAuditEventRepository auditEventRepository;

    @Override
    public void recordAllowed(String traceId, AiFeatureType featureType, UUID userId, UUID courseId, UUID lessonId) {
        save(AiAuditEvent.builder()
                .traceId(traceId)
                .featureType(featureType)
                .userId(userId)
                .courseId(courseId)
                .lessonId(lessonId)
                .status(STATUS_ALLOWED)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    public void recordBlocked(String traceId, AiFeatureType featureType, UUID userId, UUID courseId, UUID lessonId, String reason) {
        save(AiAuditEvent.builder()
                .traceId(traceId)
                .featureType(featureType)
                .userId(userId)
                .courseId(courseId)
                .lessonId(lessonId)
                .status(STATUS_BLOCKED)
                .reason(reason)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    public void recordFailed(String traceId, AiFeatureType featureType, UUID userId, UUID courseId, UUID lessonId, String reason) {
        save(AiAuditEvent.builder()
                .traceId(traceId)
                .featureType(featureType)
                .userId(userId)
                .courseId(courseId)
                .lessonId(lessonId)
                .status(STATUS_FAILED)
                .reason(reason)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    public void recordCompleted(String traceId, AiFeatureType featureType, UUID userId, UUID courseId, UUID lessonId, AiGenerateResponse response) {
        save(AiAuditEvent.builder()
                .traceId(traceId)
                .featureType(featureType)
                .userId(userId)
                .courseId(courseId)
                .lessonId(lessonId)
                .status(STATUS_COMPLETED)
                .provider(response.getProvider())
                .model(response.getModel())
                .inputTokens(response.getInputTokens())
                .outputTokens(response.getOutputTokens())
                .totalTokens(response.getTotalTokens())
                .estimatedCost(response.getEstimatedCost())
                .latencyMs(response.getLatencyMs())
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void save(AiAuditEvent event) {
        try {
            auditEventRepository.save(event);
            log.info("AI audit event status={} feature={} userId={} traceId={}",
                    event.getStatus(), event.getFeatureType(), event.getUserId(), event.getTraceId());
        } catch (RuntimeException ex) {
            log.error("Failed to persist AI audit event traceId={}", event.getTraceId(), ex);
        }
    }
}
