package com.mentify.service;

import com.mentify.dto.TeacherSearchResponse;
import com.mentify.payload.response.ApiResponse;

public interface TeacherSearchService {
    ApiResponse<TeacherSearchResponse> search(
            String query, String types, int limit, String authorizationHeader);
}
