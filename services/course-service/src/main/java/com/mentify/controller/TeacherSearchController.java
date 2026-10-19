package com.mentify.controller;

import com.mentify.dto.TeacherSearchResultResponse;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
public class TeacherSearchController {
    private final TeacherSearchService searchService;

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<TeacherSearchResultResponse>>> search(
            @RequestParam String q,
            @RequestParam(required = false, defaultValue = "all") String type,
            @RequestHeader("Authorization") String authorizationHeader) {
        ApiResponse<List<TeacherSearchResultResponse>> response = searchService.search(q, type, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
