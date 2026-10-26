package com.mentify.ai.dto.tool;

import java.time.LocalDateTime;
import java.util.List;

public record SystemHealthOverview(
        LocalDateTime checkedAt,
        int totalServices,
        int healthyServices,
        int unhealthyServices,
        List<ServiceHealth> services
) {

    public record ServiceHealth(
            String service,
            String status,
            long latencyMs,
            String detail
    ) {
    }
}
