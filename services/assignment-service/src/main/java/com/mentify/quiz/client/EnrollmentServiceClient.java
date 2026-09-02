package com.mentify.quiz.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "entrollment-service", path = "/api/v1/enrollments")
public interface EnrollmentServiceClient {

    @GetMapping("/students/{studentId}/courses/{courseId}/exists")
    boolean isStudentEnrolledInCourse(
            @PathVariable UUID studentId,
            @PathVariable UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
