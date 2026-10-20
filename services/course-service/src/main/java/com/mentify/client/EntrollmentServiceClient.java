package com.mentify.client;

import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.UUID;
import java.util.List;

@FeignClient(name = "entrollment-service", path = "/api/v1/enrollments")
public interface EntrollmentServiceClient {

    @GetMapping("/courses/{courseId}/students")
    ApiResponse<List<UUID>> getEnrolledStudentIdsByCourse(
            @PathVariable("courseId") UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader
    );

    @GetMapping("/students/{studentId}/courses/{courseId}/exists")
    boolean isStudentEnrolledInCourse(
            @PathVariable("studentId") UUID studentId,
            @PathVariable("courseId") UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader
    );

    @org.springframework.web.bind.annotation.GetMapping("/students/{studentId}/courses")
    ApiResponse<List<UUID>> getStudentCourseIds(
            @org.springframework.web.bind.annotation.PathVariable("studentId") UUID studentId,
            @org.springframework.web.bind.annotation.RequestHeader("Authorization") String authorizationHeader
    );
}
