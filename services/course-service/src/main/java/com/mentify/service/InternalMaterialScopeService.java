package com.mentify.service;

import com.mentify.dto.MaterialScopeResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.UUID;

public interface InternalMaterialScopeService {

    ApiResponse<MaterialScopeResponse> getCourseMaterialScope(UUID courseId, String authorizationHeader);

    ApiResponse<MaterialScopeResponse> getModuleMaterialScope(UUID moduleId, String authorizationHeader);

    ApiResponse<MaterialScopeResponse> getLessonMaterialScope(UUID lessonId, String authorizationHeader);
}
