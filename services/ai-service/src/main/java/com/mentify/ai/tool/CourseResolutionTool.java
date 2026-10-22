package com.mentify.ai.tool;

import com.mentify.ai.client.CourseSearchClient;
import com.mentify.ai.dto.tool.CourseSearchResponse;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseResolutionTool {

    public static final int RESULT_LIMIT = 5;

    private final CourseSearchClient courseSearchClient;

    public List<CourseSearchResponse.CourseSearchResult> resolve(String query, String authorizationHeader) {
        log.info("Resolving AI course query={}", query);
        ApiResponse<CourseSearchResponse> response = courseSearchClient.searchCourses(
                query, "course", RESULT_LIMIT, authorizationHeader);
        if (response == null || response.getData() == null || response.getData().results() == null) {
            return List.of();
        }
        return response.getData().results().stream()
                .filter(result -> "COURSE".equalsIgnoreCase(result.type()))
                .toList();
    }
}
