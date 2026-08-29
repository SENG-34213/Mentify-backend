package com.mentify.service;

import com.mentify.dto.LearningMaterialRequest;
import com.mentify.dto.LearningMaterialResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.UUID;

public interface LearningMaterialService {

    ApiResponse<LearningMaterialResponse> createLearningMaterial(
            UUID courseId,
            UUID moduleId,
            LearningMaterialRequest request
    );

    ApiResponse<LearningMaterialResponse> updateLearningMaterial(
            UUID courseId,
            UUID moduleId,
            UUID materialId,
            LearningMaterialRequest request
    );

    ApiResponse<Object> deleteLearningMaterial(UUID courseId, UUID moduleId, UUID materialId);
}
