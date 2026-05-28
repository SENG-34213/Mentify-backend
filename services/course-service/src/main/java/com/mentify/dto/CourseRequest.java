package com.mentify.dto;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRequest {

    @NotBlank(message = "Course name is required")
    @Size(max = 150, message = "Course name must not exceed 150 characters")
    private String courseName;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String courseDescription;

    private String courseThumbnail;

    @NotNull(message = "Monthly fee is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Monthly fee must be greater than zero")
    private BigDecimal courseFeeMonthly;

    @NotBlank(message = "Grade ID is required")
    private String gradeId;
}
