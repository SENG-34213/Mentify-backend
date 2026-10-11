package com.mentify.calendar.client;

import com.mentify.calendar.client.dto.CourseLookupResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;
import java.util.List;

@FeignClient(name = "course-service", url = "${mentify.clients.course-service.url:}", path = "/api/v1/course")
public interface CourseServiceClient {

    @GetMapping("/{courseId}/lookup")
    ApiResponse<CourseLookupResponse> getCourseById(
            @PathVariable("courseId") UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader);

    @GetMapping("/my-courses")
    ApiResponse<List<CourseLookupResponse>> getMyCourses(
            @RequestHeader("Authorization") String authorizationHeader);
}
