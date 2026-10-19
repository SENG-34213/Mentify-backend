package com.mentify.client.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class ExamResultSheetResponse {
    private UUID examId;
    private UUID courseId;
    private BigDecimal totalMarks;
    private List<ExamResultResponse> results;
}
