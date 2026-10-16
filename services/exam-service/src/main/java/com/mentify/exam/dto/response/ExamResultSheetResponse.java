package com.mentify.exam.dto.response;

import com.mentify.exam.enums.ExamStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class ExamResultSheetResponse {
    UUID examId;
    UUID courseId;
    String title;
    ExamStatus status;
    BigDecimal totalMarks;
    BigDecimal passMarks;
    List<ExamResultResponse> results;
}