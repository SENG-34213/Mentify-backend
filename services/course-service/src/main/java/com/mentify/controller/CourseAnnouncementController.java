package com.mentify.controller;

import com.mentify.dto.AnnouncementCreateRequest;
import com.mentify.dto.AnnouncementResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.CourseAnnouncementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
@Validated
public class CourseAnnouncementController {

    private final CourseAnnouncementService announcementService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Page<AnnouncementResponse>>> getMyAnnouncements(
            @RequestParam(required = false) UUID courseId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestHeader("Authorization") String authorizationHeader) {
        return ResponseEntity.ok(ApiResponse.success(200, "Announcements fetched successfully",
                announcementService.getMyAnnouncements(courseId, page, size, authorizationHeader)));
    }

    @PostMapping("/courses/{courseId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> createAnnouncement(
            @PathVariable UUID courseId,
            @Valid @RequestBody AnnouncementCreateRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(201,
                "Announcement created successfully", announcementService.create(courseId, request, authorizationHeader)));
    }
}
