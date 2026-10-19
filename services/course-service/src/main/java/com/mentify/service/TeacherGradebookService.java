package com.mentify.service;

import com.mentify.dto.TeacherGradebookResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.UUID;

public interface TeacherGradebookService {
    ApiResponse<TeacherGradebookResponse> getGradebook(UUID courseId, String authorizationHeader);
}
