package com.mentify.ai.dto.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentOverviewToolResult {

    private long activeStudentCount;
    private long activeEnrollmentCount;
    private long studentCourseEnrollmentCount;
    private List<CourseEnrollmentCount> enrollmentsByCourse;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseEnrollmentCount {
        private UUID courseId;
        private String courseName;
        private long enrolledStudentCount;
    }
}
