package com.mentify.service;

import com.mentify.dto.CourseProgressResponse;
import com.mentify.dto.LessonProgressResponse;
import com.mentify.dto.LessonProgressUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface StudentProgressService {
    List<LessonProgressResponse> getMyProgress();
    CourseProgressResponse getCourseProgress(UUID courseId, String authorizationHeader);
    LessonProgressResponse updateLessonProgress(UUID lessonId, LessonProgressUpdateRequest request,
                                                String authorizationHeader);
}
