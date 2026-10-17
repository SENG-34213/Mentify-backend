package com.mentify.ai.tool;

import com.mentify.ai.client.EnrollmentAnalyticsClient;
import com.mentify.ai.client.EnrollmentCourseCatalogClient;
import com.mentify.ai.dto.tool.ActiveEnrollmentRecord;
import com.mentify.ai.dto.tool.EnrollmentCourseDetails;
import com.mentify.payload.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentToolsTest {

    private static final String AUTHORIZATION_HEADER = "Bearer token";

    @Mock
    private EnrollmentAnalyticsClient enrollmentAnalyticsClient;

    @Mock
    private EnrollmentCourseCatalogClient courseCatalogClient;

    @Test
    void myEnrollmentCoursesToolUsesUserCoursesAndResolvesNames() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        when(enrollmentAnalyticsClient.getMyActiveCourseIds(studentId, AUTHORIZATION_HEADER))
                .thenReturn(ApiResponse.success(200, "ok", List.of(courseId)));
        when(courseCatalogClient.getCourseDetails(courseId, AUTHORIZATION_HEADER))
                .thenReturn(ApiResponse.success(200, "ok", EnrollmentCourseDetails.builder()
                        .id(courseId)
                        .courseName("Biology")
                        .subject("Science")
                        .courseStatus("ACTIVE")
                        .build()));

        MyEnrollmentCoursesTool tool = new MyEnrollmentCoursesTool(
                enrollmentAnalyticsClient, courseCatalogClient);

        var result = tool.execute(studentId, AUTHORIZATION_HEADER);

        assertThat(result.getCourses()).hasSize(1);
        assertThat(result.getCourses().get(0).getCourseId()).isEqualTo(courseId);
        assertThat(result.getCourses().get(0).getCourseName()).isEqualTo("Biology");
        verify(enrollmentAnalyticsClient).getMyActiveCourseIds(studentId, AUTHORIZATION_HEADER);
        verify(courseCatalogClient).getCourseDetails(courseId, AUTHORIZATION_HEADER);
    }

    @Test
    void enrollmentOverviewAggregatesCountsWithoutReturningStudentIdentities() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        when(enrollmentAnalyticsClient.getActiveEnrollments(AUTHORIZATION_HEADER))
                .thenReturn(ApiResponse.success(200, "ok", List.of(
                        ActiveEnrollmentRecord.builder().studentId(studentId).courseIds(Set.of(courseId)).build(),
                        ActiveEnrollmentRecord.builder().studentId(studentId).courseIds(Set.of(courseId)).build()
                )));
        when(courseCatalogClient.getCourseDetails(courseId, AUTHORIZATION_HEADER))
                .thenReturn(ApiResponse.success(200, "ok", EnrollmentCourseDetails.builder()
                        .id(courseId)
                        .courseName("Biology")
                        .build()));

        EnrollmentOverviewTool tool = new EnrollmentOverviewTool(
                enrollmentAnalyticsClient, courseCatalogClient);

        var result = tool.execute(AUTHORIZATION_HEADER);

        assertThat(result.getActiveStudentCount()).isEqualTo(1);
        assertThat(result.getActiveEnrollmentCount()).isEqualTo(2);
        assertThat(result.getStudentCourseEnrollmentCount()).isEqualTo(1);
        assertThat(result.getEnrollmentsByCourse()).singleElement()
                .satisfies(course -> {
                    assertThat(course.getCourseName()).isEqualTo("Biology");
                    assertThat(course.getEnrolledStudentCount()).isEqualTo(1);
                });
        assertThat(result.toString()).doesNotContain(studentId.toString());
        verify(enrollmentAnalyticsClient).getActiveEnrollments(AUTHORIZATION_HEADER);
        verify(courseCatalogClient).getCourseDetails(courseId, AUTHORIZATION_HEADER);
    }
}
