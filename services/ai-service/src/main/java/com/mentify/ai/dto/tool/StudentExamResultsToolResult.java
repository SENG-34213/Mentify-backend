package com.mentify.ai.dto.tool;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentExamResultsToolResult {

    private List<ExamResultSummary> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExamResultSummary {
        private UUID examId;
        private UUID courseId;
        private String title;
        private LocalDate examDate;
        private String type;
        private String examStatus;
        private BigDecimal totalMarks;
        private BigDecimal passMarks;
        private String attendanceStatus;
        private BigDecimal marksObtained;
        private String resultStatus;
        private String grade;
        private String remarks;
    }
}
