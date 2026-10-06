package com.mentify.exam.client;

import org.springframework.cloud.openfeign.FeignClient;

/** Placeholder for Enrollment Service integration; operations are added in later tickets. */
@FeignClient(
        name = "entrollment-service",
        url = "${mentify.clients.enrollment-service.url:}",
        path = "/api/v1/enrollments"
)
public interface EnrollmentServiceClient {
}
