package com.mentify.service;

import com.mentify.dto.LessonCreateRequest;
import com.mentify.dto.LessonResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.List;
import java.util.UUID;

public interface LessonService {

    ApiResponse<LessonResponse> createLesson(UUID courseId, UUID moduleId, LessonCreateRequest request);

    ApiResponse<LessonResponse> updateLesson(UUID courseId, UUID moduleId, UUID lessonId, LessonCreateRequest request);

    ApiResponse<LessonResponse> getLesson(UUID courseId, UUID moduleId, UUID lessonId);

    ApiResponse<List<LessonResponse>> getLessons(UUID courseId, UUID moduleId);

    ApiResponse<Object> deleteLesson(UUID courseId, UUID moduleId, UUID lessonId);
}
