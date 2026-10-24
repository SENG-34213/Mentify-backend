package com.mentify.quiz.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import com.mentify.payload.response.ApiResponse;

import java.util.List;

import java.util.UUID;

@FeignClient(name = "entrollment-service", path = "/api/v1/enrollments")
public interface EnrollmentServiceClient {

    @GetMapping("/students/{studentId}/courses")
    ApiResponse<List<UUID>> getStudentCourseIds(
            @PathVariable("studentId") UUID studentId,
            @RequestHeader("Authorization") String authorizationHeader
    );

    @GetMapping("/courses/{courseId}/students")
    ApiResponse<List<UUID>> getEnrolledStudentIdsByCourse(
            @PathVariable("courseId") UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader
    );

    @GetMapping("/students/{studentId}/courses/{courseId}/exists")
    boolean isStudentEnrolledInCourse(
            @PathVariable UUID studentId,
            @PathVariable UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
