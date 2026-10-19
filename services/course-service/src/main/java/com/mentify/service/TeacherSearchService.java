package com.mentify.service;

import com.mentify.dto.TeacherSearchResultResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.List;

public interface TeacherSearchService {
    ApiResponse<List<TeacherSearchResultResponse>> search(
            String query, String type, String authorizationHeader);
}
