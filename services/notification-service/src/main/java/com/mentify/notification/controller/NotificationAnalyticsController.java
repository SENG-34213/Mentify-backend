package com.mentify.notification.controller;

import com.mentify.notification.dto.NotificationAnalyticsOverview;
import com.mentify.notification.entity.EmailDeliveryStatus;
import com.mentify.notification.repository.NotificationRepository;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/analytics/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class NotificationAnalyticsController {

    private final NotificationRepository notificationRepository;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<NotificationAnalyticsOverview>> getOverview() {
        NotificationAnalyticsOverview overview = new NotificationAnalyticsOverview(
                notificationRepository.count(),
                notificationRepository.countByReadAtIsNull(),
                notificationRepository.countByEmailStatus(EmailDeliveryStatus.PENDING),
                notificationRepository.countByEmailStatus(EmailDeliveryStatus.SENT),
                notificationRepository.countByEmailStatus(EmailDeliveryStatus.FAILED)
        );
        return ResponseEntity.ok(ApiResponse.success(200, "Notification analytics overview fetched successfully", overview));
    }
}
