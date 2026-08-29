package com.mentify.service;

import com.mentify.dto.ModuleRequest;
import com.mentify.dto.ModuleResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.UUID;

public interface ModuleService {

    ApiResponse<ModuleResponse> createModule(UUID courseId, ModuleRequest request);

    ApiResponse<ModuleResponse> updateModule(UUID courseId, UUID moduleId, ModuleRequest request);

    ApiResponse<Object> deleteModule(UUID courseId, UUID moduleId);
}
