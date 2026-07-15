package com.mentify.service;

import com.mentify.dto.CourseRequest;
import com.mentify.dto.CourseResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.UUID;

public interface CourseService {
    ApiResponse<CourseResponse> createCourse(CourseRequest request);

    ApiResponse<CourseResponse> updateCourse(UUID courseId, CourseRequest request);
}
