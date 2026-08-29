package com.mentify.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

//this is a test file

@RestController
@RequestMapping("/api/v1/auth")
public class AuthTestController {

    @GetMapping("/public-test")
    public Map<String, String> publicTest() {
        return Map.of(
                "status", "success",
                "message", "Public endpoint is working"
        );
    }

    @GetMapping("/protected-test")
    public Map<String, Object> protectedTest(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "success");
        response.put("message", "Protected endpoint is working");
        response.put("userId", jwt.getSubject());
        response.put("email", jwt.getClaimAsString("email"));
        response.put("username", jwt.getClaimAsString("preferred_username"));
        return response;
    }

    @GetMapping("/admin-test")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> adminTest() {
        return Map.of(
                "status", "success",
                "message", "Admin authorization is working"
        );
    }

    @GetMapping("/student-test")
    @PreAuthorize("hasRole('STUDENT')")
    public Map<String, String> studentTest() {
        return Map.of(
                "status", "success",
                "message", "Student authorization is working"
        );
    }
}
