package com.mentify.exam.dto.request;

import com.mentify.exam.enums.ExamType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** Full replacement of the editable exam fields. The owning course and protected fields cannot be changed. */
@Data
public class UpdateExamRequest {

    @NotBlank(message = "title is required")
    @Size(max = 200, message = "title must not exceed 200 characters")
    private String title;

    @Size(max = 2000, message = "description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "examDate is required")
    private LocalDate examDate;

    @NotNull(message = "startTime is required")
    private LocalTime startTime;

    @NotNull(message = "endTime is required")
    private LocalTime endTime;

    @NotNull(message = "totalMarks is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "totalMarks must be greater than 0")
    private BigDecimal totalMarks;

    @NotNull(message = "passMarks is required")
    @DecimalMin(value = "0.00", message = "passMarks must not be negative")
    private BigDecimal passMarks;

    @NotNull(message = "type is required")
    private ExamType type;

    @AssertTrue(message = "endTime must be after startTime")
    public boolean isTimeRangeValid() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }

    @AssertTrue(message = "passMarks must not exceed totalMarks")
    public boolean isPassMarksWithinTotal() {
        return totalMarks == null || passMarks == null || passMarks.compareTo(totalMarks) <= 0;
    }
}
