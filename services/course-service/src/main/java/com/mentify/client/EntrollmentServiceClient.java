package com.mentify.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "entrollment-service", path = "/api/v1/enrollments")
public interface EntrollmentServiceClient {

    @GetMapping("/students/{studentId}/courses/{courseId}/exists")
    boolean isStudentEnrolledInCourse(
            @PathVariable("studentId") UUID studentId,
            @PathVariable("courseId") UUID courseId
    );
}
