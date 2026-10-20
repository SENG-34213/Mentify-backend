package com.mentify.controller;

import com.mentify.dto.TeacherSearchResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.TeacherSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
public class TeacherSearchController {
    private final TeacherSearchService searchService;

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TeacherSearchResponse>> search(
            @RequestParam String q,
            @RequestParam(required = false, defaultValue = "course,student,assignment,quiz,exam") String types,
            @RequestParam(required = false, defaultValue = "50") int limit,
            @RequestHeader("Authorization") String authorizationHeader) {
        ApiResponse<TeacherSearchResponse> response = searchService.search(q, types, limit, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
