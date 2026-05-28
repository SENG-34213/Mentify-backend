package com.mentify.dto;

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
    private String teacherId;
    private String gradeId;
    private boolean isPublished;
    private LocalDate publishedDate;
    private boolean isVisible;
    private int numberOfStudents;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
