package com.mentify.notification.controller;

import com.mentify.payload.response.ApiResponse;
import com.mentify.notification.dto.NotificationPageResponse;
import com.mentify.notification.dto.NotificationResponse;
import com.mentify.notification.service.NotificationManagementService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationManagementService notificationManagementService;

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationPageResponse>> getNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        NotificationPageResponse response = notificationManagementService.getNotifications(
                recipientId(jwt),
                page,
                size,
                unreadOnly
        );
        return ResponseEntity.ok(ApiResponse.success(200, "Notifications fetched successfully", response));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        long unreadCount = notificationManagementService.getUnreadCount(recipientId(jwt));
        return ResponseEntity.ok(ApiResponse.success(200, "Unread notification count fetched successfully", unreadCount));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID notificationId
    ) {
        NotificationResponse response = notificationManagementService.markRead(recipientId(jwt), notificationId);
        return ResponseEntity.ok(ApiResponse.success(200, "Notification marked as read", response));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Integer>> markAllRead(@AuthenticationPrincipal Jwt jwt) {
        int updated = notificationManagementService.markAllRead(recipientId(jwt));
        return ResponseEntity.ok(ApiResponse.success(200, "Notifications marked as read", updated));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID notificationId
    ) {
        notificationManagementService.delete(recipientId(jwt), notificationId);
        return ResponseEntity.ok(ApiResponse.success(200, "Notification deleted", null));
    }

    private String recipientId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new AccessDeniedException("A valid authenticated user is required");
        }
        return jwt.getSubject();
    }
}
