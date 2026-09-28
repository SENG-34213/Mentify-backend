package com.mentify.ai.entity;

import com.mentify.ai.enums.AiFeatureType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_audit_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String traceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private AiFeatureType featureType;

    @Column(nullable = false)
    private UUID userId;

    private UUID courseId;
    private UUID lessonId;

    @Column(nullable = false, length = 64)
    private String status;

    @Column(length = 128)
    private String reason;

    @Column(length = 64)
    private String provider;

    @Column(length = 128)
    private String model;

    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private BigDecimal estimatedCost;
    private Long latencyMs;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
