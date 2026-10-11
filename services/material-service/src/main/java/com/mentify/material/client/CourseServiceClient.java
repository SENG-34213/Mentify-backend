package com.mentify.material.client;

import com.mentify.material.client.dto.CourseLookupResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "course-service", path = "/api/v1/course")
public interface CourseServiceClient {

    @GetMapping("/{courseId}/lookup")
    ApiResponse<CourseLookupResponse> getCourseById(
            @PathVariable UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader
    );

    @GetMapping("/my-courses")
    ApiResponse<List<CourseLookupResponse>> getMyCourses(
            @RequestHeader("Authorization") String authorizationHeader
    );
}
