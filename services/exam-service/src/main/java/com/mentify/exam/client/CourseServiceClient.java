package com.mentify.exam.client;

import org.springframework.cloud.openfeign.FeignClient;

/** Placeholder for Course Service integration; operations are added in later tickets. */
@FeignClient(
        name = "course-service",
        url = "${mentify.clients.course-service.url:}",
        path = "/api/v1/course"
)
public interface CourseServiceClient {
}
