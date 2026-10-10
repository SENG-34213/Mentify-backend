package com.mentify.service;

import com.mentify.dto.GradeRequest;
import com.mentify.dto.GradeResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.List;
import java.util.UUID;

public interface GradeService {

    ApiResponse<GradeResponse> createGrade(GradeRequest request);

    ApiResponse<GradeResponse> updateGrade(UUID gradeId, GradeRequest request);

    ApiResponse<GradeResponse> getGradeById(UUID gradeId);

    ApiResponse<List<GradeResponse>> getAllGrades();

    ApiResponse<Object> deleteGrade(UUID gradeId);
}
