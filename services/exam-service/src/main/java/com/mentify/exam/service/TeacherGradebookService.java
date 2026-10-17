package com.mentify.exam.service;

import com.mentify.exam.dto.response.TeacherGradebookResponse;

import java.util.UUID;

public interface TeacherGradebookService {
    TeacherGradebookResponse getCourseGradebook(UUID courseId, String authorizationHeader);
}
