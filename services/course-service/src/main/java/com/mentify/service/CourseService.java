package com.mentify.service;

import com.mentify.dto.CourseRequest;
import com.mentify.dto.CourseResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.UUID;

public interface CourseService {
    ApiResponse<CourseResponse> createCourse(CourseRequest request);

    ApiResponse<CourseResponse> updateCourse(UUID courseId, CourseRequest request);

    ApiResponse<CourseResponse> getCourseById(UUID courseId);

    ApiResponse<Object> deleteCourse(UUID courseId);
}
