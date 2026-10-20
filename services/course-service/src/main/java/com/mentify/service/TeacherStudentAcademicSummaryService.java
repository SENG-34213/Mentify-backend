package com.mentify.service;

import com.mentify.dto.TeacherStudentAcademicSummaryResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.UUID;

public interface TeacherStudentAcademicSummaryService {
    ApiResponse<TeacherStudentAcademicSummaryResponse> getSummary(
            UUID studentId, UUID courseId, String authorizationHeader);
}
