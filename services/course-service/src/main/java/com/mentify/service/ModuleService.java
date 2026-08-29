package com.mentify.service;

import com.mentify.dto.ModuleCreateRequest;
import com.mentify.dto.ModuleResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.UUID;

public interface ModuleService {

    ApiResponse<ModuleResponse> createModule(UUID courseId, ModuleCreateRequest request);

    ApiResponse<ModuleResponse> updateModule(UUID courseId, UUID moduleId, ModuleCreateRequest request);

    ApiResponse<Object> deleteModule(UUID courseId, UUID moduleId);
}
