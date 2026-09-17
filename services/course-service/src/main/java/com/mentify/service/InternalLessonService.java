package com.mentify.service;

import com.mentify.dto.InternalLessonResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.UUID;

public interface InternalLessonService {
    ApiResponse<InternalLessonResponse> getLessonForAi(UUID lessonId);
}
