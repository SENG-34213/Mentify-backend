package com.mentify.ai.dto.response;
 
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
import java.math.BigDecimal;
import java.time.LocalDateTime;
 
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGenerateResponse {
 
    private String content;
    private String provider;
    private String model;
    private LocalDateTime generatedAt;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private BigDecimal estimatedCost;
    private Long latencyMs;
    private String finishReason;
    private String providerRequestId;
}
