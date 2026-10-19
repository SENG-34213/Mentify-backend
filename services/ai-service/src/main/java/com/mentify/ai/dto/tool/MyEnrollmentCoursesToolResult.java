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
public class MyEnrollmentCoursesToolResult {

    private List<EnrolledCourse> courses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrolledCourse {
        private UUID courseId;
        private String courseName;
        private String subject;
        private String courseStatus;
    }
}
