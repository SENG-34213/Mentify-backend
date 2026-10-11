package com.mentify.material.client;

import com.mentify.material.client.dto.CourseLookupResponse;
import com.mentify.material.client.dto.MaterialScopeLookupResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "course-service")
public interface CourseServiceClient {

    @GetMapping("/api/v1/course/{courseId}/lookup")
    ApiResponse<CourseLookupResponse> getCourseById(
            @PathVariable UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader
    );

    @GetMapping("/api/v1/course/my-courses")
    ApiResponse<List<CourseLookupResponse>> getMyCourses(
            @RequestHeader("Authorization") String authorizationHeader
    );

    @GetMapping("/api/v1/internal/material-scopes/courses/{courseId}")
    ApiResponse<MaterialScopeLookupResponse> getCourseMaterialScope(
            @PathVariable UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader
    );

    @GetMapping("/api/v1/internal/material-scopes/modules/{moduleId}")
    ApiResponse<MaterialScopeLookupResponse> getModuleMaterialScope(
            @PathVariable UUID moduleId,
            @RequestHeader("Authorization") String authorizationHeader
    );

    @GetMapping("/api/v1/internal/material-scopes/lessons/{lessonId}")
    ApiResponse<MaterialScopeLookupResponse> getLessonMaterialScope(
            @PathVariable UUID lessonId,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
