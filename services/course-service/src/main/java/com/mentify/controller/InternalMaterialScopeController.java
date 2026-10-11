package com.mentify.controller;

import com.mentify.dto.MaterialScopeResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.InternalMaterialScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/material-scopes")
@RequiredArgsConstructor
public class InternalMaterialScopeController {

    private final InternalMaterialScopeService internalMaterialScopeService;

    @GetMapping("/courses/{courseId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MaterialScopeResponse>> getCourseMaterialScope(
            @PathVariable UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<MaterialScopeResponse> response =
                internalMaterialScopeService.getCourseMaterialScope(courseId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/modules/{moduleId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MaterialScopeResponse>> getModuleMaterialScope(
            @PathVariable UUID moduleId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<MaterialScopeResponse> response =
                internalMaterialScopeService.getModuleMaterialScope(moduleId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/lessons/{lessonId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MaterialScopeResponse>> getLessonMaterialScope(
            @PathVariable UUID lessonId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<MaterialScopeResponse> response =
                internalMaterialScopeService.getLessonMaterialScope(lessonId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
