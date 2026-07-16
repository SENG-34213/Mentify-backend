package com.mentify.client;

import com.mentify.client.dto.CourseLookupResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "course-service", path = "/api/v1/course")
public interface CourseServiceClient {

    @GetMapping("/{courseId}")
    ApiResponse<CourseLookupResponse> getCourseById(
            @PathVariable UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader
    );
}

