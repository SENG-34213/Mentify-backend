package com.mentify.exam.dto.response;

import com.mentify.exam.enums.AttendanceStatus;
import com.mentify.exam.enums.ResultStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class ExamResultResponse {
    UUID studentId;
    AttendanceStatus attendanceStatus;
    BigDecimal marksObtained;
    ResultStatus resultStatus;
    String grade;
    String remarks;
    UUID markedBy;
    LocalDateTime markedAt;
    LocalDateTime updatedAt;
}