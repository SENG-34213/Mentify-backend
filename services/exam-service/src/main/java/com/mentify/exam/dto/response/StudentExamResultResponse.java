package com.mentify.exam.dto.response;

import com.mentify.exam.enums.AttendanceStatus;
import com.mentify.exam.enums.ExamStatus;
import com.mentify.exam.enums.ExamType;
import com.mentify.exam.enums.ResultStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Value
@Builder
public class StudentExamResultResponse {
    UUID examId;
    UUID courseId;
    String title;
    String description;
    String venue;
    LocalDate examDate;
    LocalTime startTime;
    LocalTime endTime;
    ExamType type;
    ExamStatus examStatus;
    BigDecimal totalMarks;
    BigDecimal passMarks;
    AttendanceStatus attendanceStatus;
    BigDecimal marksObtained;
    ResultStatus resultStatus;
    String grade;
    String remarks;
}
