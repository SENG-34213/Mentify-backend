package com.mentify.controller;

import com.mentify.dto.TeacherGradebookResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.TeacherGradebookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
public class TeacherGradebookController {

    private final TeacherGradebookService gradebookService;

    @GetMapping("/gradebook")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TeacherGradebookResponse>> getGradebook(
            @RequestParam UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader) {
        ApiResponse<TeacherGradebookResponse> response = gradebookService
                .getGradebook(courseId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
