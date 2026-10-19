package com.mentify.service;

import com.mentify.dto.LearningMaterialCreateRequest;
import com.mentify.dto.LearningMaterialResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface LearningMaterialService {

    ApiResponse<LearningMaterialResponse> uploadLearningMaterial(
            UUID courseId,
            UUID moduleId,
            UUID lessonId,
            String title,
            String description,
            MultipartFile file,
            String authorizationHeader
    );

    ApiResponse<LearningMaterialResponse> createLearningMaterial(
            UUID courseId,
            UUID moduleId,
            LearningMaterialCreateRequest request,
            String authorizationHeader
    );

    ApiResponse<LearningMaterialResponse> updateLearningMaterial(
            UUID courseId,
            UUID moduleId,
            UUID materialId,
            LearningMaterialCreateRequest request,
            String authorizationHeader
    );

    ApiResponse<LearningMaterialResponse> getLearningMaterial(
            UUID courseId,
            UUID moduleId,
            UUID materialId,
            String authorizationHeader
    );

    ApiResponse<List<LearningMaterialResponse>> getLearningMaterials(
            UUID courseId,
            UUID moduleId,
            String authorizationHeader
    );

    ApiResponse<Object> deleteLearningMaterial(UUID courseId, UUID moduleId, UUID materialId);
}
