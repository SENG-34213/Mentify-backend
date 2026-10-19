package com.mentify.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class TeacherGradebookSummaryResponse {
    long studentCount;
    BigDecimal averagePercentage;
    BigDecimal highestPercentage;
    long atRiskCount;
}
