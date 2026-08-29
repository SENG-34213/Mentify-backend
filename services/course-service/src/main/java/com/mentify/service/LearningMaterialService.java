package com.mentify.service;

import com.mentify.dto.LearningMaterialCreateRequest;
import com.mentify.dto.LearningMaterialResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.List;
import java.util.UUID;

public interface LearningMaterialService {

    ApiResponse<LearningMaterialResponse> createLearningMaterial(
            UUID courseId,
            UUID moduleId,
            LearningMaterialCreateRequest request
    );

    ApiResponse<LearningMaterialResponse> updateLearningMaterial(
            UUID courseId,
            UUID moduleId,
            UUID materialId,
            LearningMaterialCreateRequest request
    );

        ApiResponse<LearningMaterialResponse> getLearningMaterial(UUID courseId, UUID moduleId, UUID materialId);

        ApiResponse<List<LearningMaterialResponse>> getLearningMaterials(UUID courseId, UUID moduleId);

    ApiResponse<Object> deleteLearningMaterial(UUID courseId, UUID moduleId, UUID materialId);
}
