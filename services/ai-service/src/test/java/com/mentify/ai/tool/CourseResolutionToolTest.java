package com.mentify.ai.tool;

import com.mentify.ai.client.CourseSearchClient;
import com.mentify.ai.dto.tool.CourseSearchResponse;
import com.mentify.payload.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseResolutionToolTest {

    private static final String AUTHORIZATION_HEADER = "Bearer token";

    @Mock
    private CourseSearchClient courseSearchClient;

    @Test
    void resolvesOnlyCourseResultsUsingAuthorizedSearch() {
        UUID courseId = UUID.randomUUID();
        CourseSearchResponse.CourseSearchResult course = new CourseSearchResponse.CourseSearchResult(
                courseId, "COURSE", "Grade 8 Java", "Java", courseId, "courses");
        CourseSearchResponse.CourseSearchResult student = new CourseSearchResponse.CourseSearchResult(
                UUID.randomUUID(), "STUDENT", "Student", "Grade 8 Java", courseId, "students");
        when(courseSearchClient.searchCourses("grade 8", "course", CourseResolutionTool.RESULT_LIMIT, AUTHORIZATION_HEADER))
                .thenReturn(ApiResponse.success(200, "ok", new CourseSearchResponse(List.of(course, student), 2)));

        CourseResolutionTool tool = new CourseResolutionTool(courseSearchClient);

        assertThat(tool.resolve("grade 8", AUTHORIZATION_HEADER)).containsExactly(course);
        verify(courseSearchClient).searchCourses("grade 8", "course", CourseResolutionTool.RESULT_LIMIT, AUTHORIZATION_HEADER);
    }
}
