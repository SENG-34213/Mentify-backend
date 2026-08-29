package com.mentify.controller;

import com.mentify.dto.ModuleRequest;
import com.mentify.dto.ModuleResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.ModuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/course/{courseId}/modules")
@RequiredArgsConstructor
public class ModuleController {

    private final ModuleService moduleService;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ModuleResponse>> createModule(
            @PathVariable UUID courseId,
            @Valid @RequestBody ModuleRequest request
    ) {
        ApiResponse<ModuleResponse> response = moduleService.createModule(courseId, request);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PutMapping("/{moduleId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ModuleResponse>> updateModule(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @Valid @RequestBody ModuleRequest request
    ) {
        ApiResponse<ModuleResponse> response = moduleService.updateModule(courseId, moduleId, request);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @DeleteMapping("/{moduleId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Object>> deleteModule(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId
    ) {
        ApiResponse<Object> response = moduleService.deleteModule(courseId, moduleId);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
