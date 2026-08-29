package com.mentify.controller;

import com.mentify.dto.LearningMaterialRequest;
import com.mentify.dto.LearningMaterialResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.LearningMaterialService;
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
@RequestMapping("/api/v1/course/{courseId}/modules/{moduleId}/learning-materials")
@RequiredArgsConstructor
public class LearningMaterialController {

    private final LearningMaterialService learningMaterialService;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<LearningMaterialResponse>> createLearningMaterial(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @Valid @RequestBody LearningMaterialRequest request
    ) {
        ApiResponse<LearningMaterialResponse> response =
                learningMaterialService.createLearningMaterial(courseId, moduleId, request);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PutMapping("/{materialId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<LearningMaterialResponse>> updateLearningMaterial(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID materialId,
            @Valid @RequestBody LearningMaterialRequest request
    ) {
        ApiResponse<LearningMaterialResponse> response =
                learningMaterialService.updateLearningMaterial(courseId, moduleId, materialId, request);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @DeleteMapping("/{materialId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Object>> deleteLearningMaterial(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID materialId
    ) {
        ApiResponse<Object> response = learningMaterialService.deleteLearningMaterial(courseId, moduleId, materialId);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
