package com.mentify.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeMonthlyFeeResponse {
    private UUID id;
    private UUID gradeId;
    private String gradeName;
    private BigDecimal monthlyFee;
    private String currencyCode;
}
