package com.mentify.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class TeacherGradeDistributionResponse {
    String grade;
    long count;
    BigDecimal percentage;
}
