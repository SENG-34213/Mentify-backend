package com.mentify.client.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ExamResultResponse {
    private UUID studentId;
    private BigDecimal marksObtained;
    private String grade;
}
