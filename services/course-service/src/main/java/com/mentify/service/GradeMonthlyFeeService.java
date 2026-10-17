package com.mentify.service;

import com.mentify.dto.GradeMonthlyFeeRequest;
import com.mentify.dto.GradeMonthlyFeeResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.List;
import java.util.UUID;

public interface GradeMonthlyFeeService {
    ApiResponse<List<GradeMonthlyFeeResponse>> getAll();
    ApiResponse<GradeMonthlyFeeResponse> getByGrade(UUID gradeId);
    ApiResponse<GradeMonthlyFeeResponse> create(UUID gradeId, GradeMonthlyFeeRequest request);
    ApiResponse<GradeMonthlyFeeResponse> update(UUID gradeId, GradeMonthlyFeeRequest request);
    ApiResponse<Object> delete(UUID gradeId);
}
