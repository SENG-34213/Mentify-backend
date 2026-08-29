package com.mentify.service;

import com.mentify.dto.LessonRequest;
import com.mentify.dto.LessonResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.UUID;

public interface LessonService {

    ApiResponse<LessonResponse> createLesson(UUID courseId, UUID moduleId, LessonRequest request);

    ApiResponse<LessonResponse> updateLesson(UUID courseId, UUID moduleId, UUID lessonId, LessonRequest request);

    ApiResponse<Object> deleteLesson(UUID courseId, UUID moduleId, UUID lessonId);
}
