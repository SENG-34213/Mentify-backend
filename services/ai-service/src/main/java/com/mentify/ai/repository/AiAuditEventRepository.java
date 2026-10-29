package com.mentify.ai.repository;

import com.mentify.ai.entity.AiAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.util.UUID;

public interface AiAuditEventRepository extends JpaRepository<AiAuditEvent, UUID> {

    long countByCreatedAtAfter(LocalDateTime since);

    long countByStatusAndCreatedAtAfter(String status, LocalDateTime since);

    @Query("select coalesce(sum(e.totalTokens), 0) from AiAuditEvent e where e.createdAt >= :since")
    Long sumTotalTokensSince(LocalDateTime since);

    @Query("select coalesce(sum(e.estimatedCost), 0) from AiAuditEvent e where e.createdAt >= :since")
    BigDecimal sumEstimatedCostSince(LocalDateTime since);

    @Query("select avg(e.latencyMs) from AiAuditEvent e where e.createdAt >= :since and e.latencyMs is not null")
    Double averageLatencyMsSince(LocalDateTime since);
}
