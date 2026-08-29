package com.mentify.service;

import com.mentify.dto.LearningMaterialCreateRequest;
import com.mentify.dto.LearningMaterialResponse;
import com.mentify.payload.response.ApiResponse;

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
}
