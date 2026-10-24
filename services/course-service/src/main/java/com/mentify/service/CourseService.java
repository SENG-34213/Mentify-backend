package com.mentify.service;

import com.mentify.dto.CourseRequest;
import com.mentify.dto.CourseResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.mentify.dto.TeacherDashboardResponse;

public interface CourseService {

    ApiResponse<TeacherDashboardResponse> getTeacherDashboard(String authorizationHeader);
    ApiResponse<CourseResponse> createCourse(CourseRequest request, String authorizationHeader);

    ApiResponse<CourseResponse> updateCourse(UUID courseId, CourseRequest request, String authorizationHeader);

    ApiResponse<CourseResponse> getCourseById(UUID courseId);
    ApiResponse<CourseResponse> getCourseById(UUID courseId, String authorizationHeader);

    ApiResponse<List<CourseResponse>> getAllCourses();
    ApiResponse<List<CourseResponse>> getAllCourses(String authorizationHeader);

    ApiResponse<List<CourseResponse>> getCoursesForTeacher(UUID teacherId);

    ApiResponse<List<CourseResponse>> getCoursesForTeacher(UUID teacherId, String authorizationHeader);

    ApiResponse<List<CourseResponse>> getCoursesByIds(Set<UUID> ids);
    ApiResponse<List<CourseResponse>> getCoursesByIds(Set<UUID> ids, String authorizationHeader);

    ApiResponse<Object> deleteCourse(UUID courseId);
}
