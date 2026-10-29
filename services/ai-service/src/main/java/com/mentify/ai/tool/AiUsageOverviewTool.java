package com.mentify.ai.tool;

import com.mentify.ai.dto.tool.AiUsageOverview;
import com.mentify.ai.repository.AiAuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiUsageOverviewTool implements AiTool<AiUsageOverview> {

    public static final String TOOL_NAME = "getAiUsageOverview";
    private static final int DEFAULT_LOOKBACK_DAYS = 30;

    private final AiAuditEventRepository auditEventRepository;
    private AiScopedTtlCache cache = new AiScopedTtlCache();

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public AiUsageOverview execute(String authorizationHeader) {
        return cache.get(TOOL_NAME, authorizationHeader, Duration.ofSeconds(30), this::load);
    }

    private AiUsageOverview load() {
        LocalDateTime since = LocalDateTime.now().minusDays(DEFAULT_LOOKBACK_DAYS);
        log.info("Loading AI tool {} since={}", TOOL_NAME, since);
        return new AiUsageOverview(
                since,
                auditEventRepository.countByCreatedAtAfter(since),
                auditEventRepository.countByStatusAndCreatedAtAfter("COMPLETED", since),
                auditEventRepository.countByStatusAndCreatedAtAfter("FAILED", since),
                auditEventRepository.countByStatusAndCreatedAtAfter("BLOCKED", since),
                valueOrZero(auditEventRepository.sumTotalTokensSince(since)),
                valueOrZero(auditEventRepository.sumEstimatedCostSince(since)),
                auditEventRepository.averageLatencyMsSince(since)
        );
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
