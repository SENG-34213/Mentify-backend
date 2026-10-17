package com.mentify.ai.dto.tool;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExamSummaryToolResult {

    private UUID examId;
    private UUID courseId;
    private String title;
    private String status;
    private BigDecimal totalMarks;
    private BigDecimal passMarks;
    private long totalStudents;
    private long presentStudents;
    private long absentStudents;
    private long passedStudents;
    private long failedStudents;
    private BigDecimal averageMarks;
    private BigDecimal highestMarks;
    private BigDecimal lowestMarks;
}
