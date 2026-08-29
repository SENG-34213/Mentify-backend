package com.mentify.controller;

import com.mentify.dto.CourseBulkLookupRequest;
import com.mentify.dto.CourseRequest;
import com.mentify.dto.CourseResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;


    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
            @Valid @RequestBody CourseRequest request) {

        ApiResponse<CourseResponse> response = courseService.createCourse(request);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PutMapping("/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(
            @PathVariable UUID courseId,
            @Valid @RequestBody CourseRequest request) {

        ApiResponse<CourseResponse> response = courseService.updateCourse(courseId, request);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourseById(@PathVariable UUID courseId) {
        ApiResponse<CourseResponse> response = courseService.getCourseById(courseId);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getCoursesByIds(
            @Valid @RequestBody CourseBulkLookupRequest request
    ) {
        ApiResponse<List<CourseResponse>> response = courseService.getCoursesByIds(request.getIds());
        return new ResponseEntity<>(response, response.getStatus());
    }

    @DeleteMapping("/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> deleteCourse(@PathVariable UUID courseId) {
        ApiResponse<Object> response = courseService.deleteCourse(courseId);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
