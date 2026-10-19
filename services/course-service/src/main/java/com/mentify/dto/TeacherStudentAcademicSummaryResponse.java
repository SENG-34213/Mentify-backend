package com.mentify.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class TeacherStudentAcademicSummaryResponse {
    UUID studentId;
    UUID courseId;
    BigDecimal attendancePercentage;
    long absentCount;
    BigDecimal assignmentAverage;
    BigDecimal quizAverage;
    BigDecimal examAverage;
    BigDecimal overallPercentage;
    BigDecimal gpa;
    long submissionsPending;
    LocalDateTime lastActiveAt;
}
