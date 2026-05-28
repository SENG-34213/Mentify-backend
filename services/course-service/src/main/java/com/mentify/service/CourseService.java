package com.mentify.service;

import com.mentify.dto.CourseRequest;
import com.mentify.dto.CourseResponse;
import com.mentify.payload.response.ApiResponse;

public interface CourseService {
    ApiResponse<CourseResponse> createCourse(CourseRequest request, String teacherId);
}
