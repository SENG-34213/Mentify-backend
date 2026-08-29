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
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRequest {

    @NotBlank(message = "Course name is required")
    @Size(max = 150, message = "Course name must not exceed 150 characters")
    private String courseName;

    @NotBlank(message = "Description is required.")
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String courseDescription;

    private String courseThumbnail;

    @NotNull(message = "Monthly fee is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Monthly fee must be greater than zero")
    private BigDecimal courseFeeMonthly;

    @NotNull(message = "Grade ID is required")
    private UUID gradeId;

    @NotNull(message = "Assigned teacher ID is required.")
    private UUID assignedTeacherId;

    @NotBlank(message = "Subject is required")
    @Size(max = 100, message = "Subject must not exceed 100 characters")
    private String subject;

    private Boolean isOnline;

    @DecimalMin(value = "0.0", inclusive = true, message = "Discount offer percent must not be negative")
    private BigDecimal discountOfferPercent;

    private Boolean isVisible;

    private Boolean isPublished;

    private UUID courseEnrollmentId;

}
