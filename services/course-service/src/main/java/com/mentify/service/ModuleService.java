package com.mentify.service;

import com.mentify.dto.ModuleCreateRequest;
import com.mentify.dto.ModuleResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.List;
import java.util.UUID;

public interface ModuleService {

    ApiResponse<ModuleResponse> createModule(UUID courseId, ModuleCreateRequest request);

    ApiResponse<ModuleResponse> updateModule(UUID courseId, UUID moduleId, ModuleCreateRequest request);

    ApiResponse<ModuleResponse> getModule(UUID courseId, UUID moduleId);

    ApiResponse<List<ModuleResponse>> getModules(UUID courseId);

    ApiResponse<Object> deleteModule(UUID courseId, UUID moduleId);
}
