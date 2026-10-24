package com.mentify.exam.dto.response;

import com.mentify.exam.enums.ExamStatus;
import com.mentify.exam.enums.ExamType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Value
@Builder
public class StudentUpcomingExamResponse {
    UUID examId;
    UUID courseId;
    String title;
    String description;
    String venue;
    LocalDate examDate;
    LocalTime startTime;
    LocalTime endTime;
    BigDecimal totalMarks;
    BigDecimal passMarks;
    ExamType type;
    ExamStatus status;
    boolean eligible;
}
