package com.mentify.quiz.controller;

import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.dto.response.TeacherGradebookQuizResponse;
import com.mentify.quiz.service.TeacherGradebookQuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TeacherGradebookQuizController {

    private final TeacherGradebookQuizService gradebookQuizService;

    @GetMapping("/api/v1/teacher/gradebook/quizzes")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<TeacherGradebookQuizResponse>>> getQuizzes(
            @RequestParam UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader) {
        ApiResponse<List<TeacherGradebookQuizResponse>> response =
                gradebookQuizService.getQuizzes(courseId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
