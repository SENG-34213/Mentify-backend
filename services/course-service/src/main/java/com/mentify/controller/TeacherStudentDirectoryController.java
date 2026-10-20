package com.mentify.controller;

import com.mentify.dto.TeacherStudentDirectoryResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.TeacherStudentDirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
public class TeacherStudentDirectoryController {

    private final TeacherStudentDirectoryService directoryService;

    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<TeacherStudentDirectoryResponse>>> getStudents(
            @RequestParam UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader) {
        ApiResponse<List<TeacherStudentDirectoryResponse>> response =
                directoryService.getStudents(courseId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
