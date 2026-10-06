package com.mentify.exam.dto.request;

import com.mentify.exam.enums.AttendanceStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/** One student's marks. resultStatus, markedBy, markedAt and updatedAt are backend-controlled and not accepted. */
@Data
public class MarkEntryRequest {

    @NotNull(message = "studentId is required")
    private UUID studentId;

    @NotNull(message = "attendanceStatus is required")
    private AttendanceStatus attendanceStatus;

    @DecimalMin(value = "0.00", message = "marksObtained must not be negative")
    @Digits(integer = 6, fraction = 2, message = "marksObtained must have at most 2 decimal places")
    private BigDecimal marksObtained;

    @Size(max = 1000, message = "remarks must not exceed 1000 characters")
    private String remarks;
}