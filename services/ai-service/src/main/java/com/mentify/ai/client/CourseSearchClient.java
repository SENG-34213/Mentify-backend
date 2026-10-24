package com.mentify.ai.client;

import com.mentify.ai.dto.tool.CourseSearchResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "course-service", contextId = "aiCourseSearchClient", path = "/api/v1/teacher")
public interface CourseSearchClient {

    @GetMapping("/search")
    ApiResponse<CourseSearchResponse> searchCourses(
            @RequestParam("q") String query,
            @RequestParam("types") String types,
            @RequestParam("limit") Integer limit,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
