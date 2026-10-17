package com.mentify.ai.tool;

import com.mentify.ai.client.EnrollmentAnalyticsClient;
import com.mentify.ai.client.EnrollmentCourseCatalogClient;
import com.mentify.ai.dto.tool.ActiveEnrollmentRecord;
import com.mentify.ai.dto.tool.EnrollmentCourseDetails;
import com.mentify.ai.dto.tool.EnrollmentOverviewToolResult;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnrollmentOverviewTool {

    public static final String TOOL_NAME = "getEnrollmentOverview";

    private final EnrollmentAnalyticsClient enrollmentAnalyticsClient;
    private final EnrollmentCourseCatalogClient courseCatalogClient;

    public String getName() {
        return TOOL_NAME;
    }

    public EnrollmentOverviewToolResult execute(String authorizationHeader) {
        log.info("Executing AI tool {}", TOOL_NAME);
        ApiResponse<List<ActiveEnrollmentRecord>> response =
                enrollmentAnalyticsClient.getActiveEnrollments(authorizationHeader);
        List<ActiveEnrollmentRecord> records = Objects.requireNonNull(
                Objects.requireNonNull(response, "Enrollment service returned no response").getData(),
                "Enrollment service returned no active enrollment list");

        Set<UUID> activeStudentIds = new HashSet<>();
        Map<UUID, Set<UUID>> studentIdsByCourse = new HashMap<>();
        for (ActiveEnrollmentRecord record : records) {
            UUID studentId = Objects.requireNonNull(record.getStudentId(),
                    "Enrollment service returned an enrollment without a student ID");
            Set<UUID> courseIds = Objects.requireNonNull(record.getCourseIds(),
                    "Enrollment service returned an enrollment without course IDs");
            activeStudentIds.add(studentId);
            for (UUID courseId : courseIds) {
                studentIdsByCourse.computeIfAbsent(courseId, ignored -> new HashSet<>()).add(studentId);
            }
        }
        long courseEnrollmentCount = studentIdsByCourse.values().stream()
                .mapToLong(Set::size)
                .sum();

        List<EnrollmentOverviewToolResult.CourseEnrollmentCount> courseCounts = new ArrayList<>();
        for (Map.Entry<UUID, Set<UUID>> entry : studentIdsByCourse.entrySet()) {
            ApiResponse<EnrollmentCourseDetails> courseResponse =
                    courseCatalogClient.getCourseDetails(entry.getKey(), authorizationHeader);
            EnrollmentCourseDetails course = Objects.requireNonNull(
                    Objects.requireNonNull(courseResponse, "Course service returned no response").getData(),
                    "Course service returned no details for course " + entry.getKey());
            courseCounts.add(EnrollmentOverviewToolResult.CourseEnrollmentCount.builder()
                    .courseId(entry.getKey())
                    .courseName(course.getCourseName())
                    .enrolledStudentCount(entry.getValue().size())
                    .build());
        }
        courseCounts.sort((first, second) -> first.getCourseId().compareTo(second.getCourseId()));

        return EnrollmentOverviewToolResult.builder()
                .activeStudentCount(activeStudentIds.size())
                .activeEnrollmentCount(records.size())
                .studentCourseEnrollmentCount(courseEnrollmentCount)
                .enrollmentsByCourse(courseCounts)
                .build();
    }
}
