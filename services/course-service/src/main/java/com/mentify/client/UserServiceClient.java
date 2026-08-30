package com.mentify.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service", path = "/api/v1/users")
public interface UserServiceClient {

    @GetMapping("/teachers/{teacherId}/exists")
    boolean isTeacherExists(@PathVariable String teacherId, @RequestHeader("Authorization") String authorizationHeader);
}
