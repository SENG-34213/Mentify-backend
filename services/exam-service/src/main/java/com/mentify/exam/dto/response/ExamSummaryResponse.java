package com.mentify.exam.dto.response;

import com.mentify.exam.enums.ExamStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class ExamSummaryResponse {
    UUID examId;
    UUID courseId;
    String title;
    ExamStatus status;
    BigDecimal totalMarks;
    BigDecimal passMarks;
    long totalStudents;
    long presentStudents;
    long absentStudents;
    long passedStudents;
    long failedStudents;
    BigDecimal averageMarks;
    BigDecimal highestMarks;
    BigDecimal lowestMarks;
}