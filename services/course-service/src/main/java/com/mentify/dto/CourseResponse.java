package com.mentify.dto;

import com.mentify.enums.CourseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {

    private UUID id;
    private String courseName;
    private String courseDescription;
    private String courseThumbnail;
    private BigDecimal courseFeeMonthly;
    private UUID assignedTeacherId;
    private UUID courseEnrollmentId;
    private UUID gradeId;
    private String subject;
    private boolean online;
    private BigDecimal discountOfferPercent;
    private boolean visible;
    private boolean isPublished;
    private CourseStatus courseStatus;
    private LocalDate publishedDate;
    private int numberOfStudents;
    private UUID createdBy;
    private UUID lastModifiedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
