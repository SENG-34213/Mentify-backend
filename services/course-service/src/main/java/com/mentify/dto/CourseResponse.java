package com.mentify.dto;

import com.mentify.enums.CourseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {

    private UUID id;
    private String courseName;
    private String courseCode;
    private String courseDescription;
    private String courseThumbnail;
    private BigDecimal gradeMonthlyFee;
    private String gradeCurrencyCode;
    /** Human-readable grade label for user-facing clients. */
    private String gradeName;
    private UUID assignedTeacherId;
    private UUID gradeId;
    private String subject;
    private String level;
    private String semester;
    private String academicYear;
    private boolean online;
    private boolean visible;
    private boolean isPublished;
    private CourseStatus courseStatus;
    private LocalDate publishedDate;
    private int numberOfStudents;
    private Long moduleCount;
    private Long lessonCount;
    private Long assignmentCount;
    private Long quizCount;
    private Long examCount;
    private BigDecimal averageGrade;
    private Integer maxCapacity;
    private Integer credits;
    private String schedule;
    private Map<String, BigDecimal> gradingWeights;
    private Set<UUID> teachingAssistantIds;
    private UUID createdBy;
    private UUID lastModifiedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
