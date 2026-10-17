package com.mentify.exam.controller;

import com.mentify.exam.dto.response.TeacherGradebookResponse;
import com.mentify.exam.service.TeacherGradebookService;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gradebook")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
public class TeacherGradebookController {

    private final TeacherGradebookService teacherGradebookService;

    @GetMapping("/courses/{courseId}")
    public ResponseEntity<ApiResponse<TeacherGradebookResponse>> getCourseGradebook(
            @PathVariable UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader) {
        return ResponseEntity.ok(ApiResponse.success(200, "Gradebook fetched successfully",
                teacherGradebookService.getCourseGradebook(courseId, authorizationHeader)));
    }
}
