package com.mentify.exam.client;

import com.mentify.exam.client.dto.CourseLookupResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

/** Course Service integration. The caller's bearer token is forwarded so Course Service authorizes the lookup. */
@FeignClient(
        name = "course-service",
        url = "${mentify.clients.course-service.url:}",
        path = "/api/v1/course"
)
public interface CourseServiceClient {

    @GetMapping("/{courseId}/lookup")
    ApiResponse<CourseLookupResponse> getCourseById(
            @PathVariable("courseId") UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader);
}
