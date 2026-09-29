package com.mentify.controller;

import com.mentify.dto.UserRegistrationOverviewResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.UserAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/users/analytics")
@RequiredArgsConstructor
public class UserAnalyticsController {

    private final UserAnalyticsService userAnalyticsService;

    @GetMapping("/registration-overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserRegistrationOverviewResponse>> getRegistrationOverview() {
        UserRegistrationOverviewResponse response = userAnalyticsService.getRegistrationOverview();
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "User registration overview fetched successfully",
                response
        ));
    }
}
