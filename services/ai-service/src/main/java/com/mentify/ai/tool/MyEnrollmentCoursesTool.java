package com.mentify.ai.tool;

import com.mentify.ai.client.EnrollmentAnalyticsClient;
import com.mentify.ai.client.EnrollmentCourseCatalogClient;
import com.mentify.ai.dto.tool.EnrollmentCourseDetails;
import com.mentify.ai.dto.tool.MyEnrollmentCoursesToolResult;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MyEnrollmentCoursesTool {

    public static final String TOOL_NAME = "getMyEnrolledCourses";

    private final EnrollmentAnalyticsClient enrollmentAnalyticsClient;
    private final EnrollmentCourseCatalogClient courseCatalogClient;

    public String getName() {
        return TOOL_NAME;
    }

    public MyEnrollmentCoursesToolResult execute(UUID studentId, String authorizationHeader) {
        log.info("Executing AI tool {}", TOOL_NAME);
        ApiResponse<List<UUID>> response =
                enrollmentAnalyticsClient.getMyActiveCourseIds(studentId, authorizationHeader);
        List<UUID> courseIds = Objects.requireNonNull(
                Objects.requireNonNull(response, "Enrollment service returned no response").getData(),
                "Enrollment service returned no active course list");

        List<MyEnrollmentCoursesToolResult.EnrolledCourse> courses = courseIds.stream()
                .sorted()
                .map(courseId -> toEnrolledCourse(courseId, authorizationHeader))
                .toList();
        return MyEnrollmentCoursesToolResult.builder().courses(courses).build();
    }

    private MyEnrollmentCoursesToolResult.EnrolledCourse toEnrolledCourse(
            UUID courseId,
            String authorizationHeader
    ) {
        ApiResponse<EnrollmentCourseDetails> response =
                courseCatalogClient.getCourseDetails(courseId, authorizationHeader);
        EnrollmentCourseDetails course = Objects.requireNonNull(
                Objects.requireNonNull(response, "Course service returned no response").getData(),
                "Course service returned no details for course " + courseId);
        return MyEnrollmentCoursesToolResult.EnrolledCourse.builder()
                .courseId(courseId)
                .courseName(course.getCourseName())
                .subject(course.getSubject())
                .courseStatus(course.getCourseStatus())
                .build();
    }
}
