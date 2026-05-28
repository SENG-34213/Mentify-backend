package com.mentify.controller;

import com.mentify.dto.CourseRequest;
import com.mentify.dto.CourseResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
            @Valid @RequestBody CourseRequest request,
            @RequestHeader("X-teacher-Id") String teacherId) {

        ApiResponse<CourseResponse> response = courseService.createCourse(request, teacherId);
        return new ResponseEntity<>(response, response.getStatus());
    }}
