package com.mentify.client;

import com.mentify.client.dto.CourseLookupResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(
        name = "course-service",
        url = "${mentify.clients.course-service.url:}",
        path = "/api/v1/course"
)
public interface CourseServiceClient {

    @GetMapping("/{courseId}/lookup")
    ApiResponse<CourseLookupResponse> getCourseById(
            @PathVariable("courseId") UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
