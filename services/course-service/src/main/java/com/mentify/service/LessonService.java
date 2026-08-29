package com.mentify.service;

import com.mentify.dto.LessonCreateRequest;
import com.mentify.dto.LessonResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.UUID;

public interface LessonService {

    ApiResponse<LessonResponse> createLesson(UUID courseId, UUID moduleId, LessonCreateRequest request);

    ApiResponse<LessonResponse> updateLesson(UUID courseId, UUID moduleId, UUID lessonId, LessonCreateRequest request);
}
