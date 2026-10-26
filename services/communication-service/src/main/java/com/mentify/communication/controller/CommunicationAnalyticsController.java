package com.mentify.communication.controller;

import com.mentify.communication.dto.response.CommunicationAnalyticsOverview;
import com.mentify.communication.enums.GroupStatus;
import com.mentify.communication.enums.MessageStatus;
import com.mentify.communication.repository.CommunicationGroupRepository;
import com.mentify.communication.repository.MessageRepository;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/analytics/communication")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class CommunicationAnalyticsController {

    private final CommunicationGroupRepository communicationGroupRepository;
    private final MessageRepository messageRepository;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<CommunicationAnalyticsOverview>> getOverview() {
        CommunicationAnalyticsOverview overview = new CommunicationAnalyticsOverview(
                communicationGroupRepository.count(),
                communicationGroupRepository.countByStatus(GroupStatus.ACTIVE),
                communicationGroupRepository.countByStatus(GroupStatus.ARCHIVED),
                messageRepository.count(),
                messageRepository.countByStatus(MessageStatus.ACTIVE),
                messageRepository.countByStatus(MessageStatus.DELETED)
        );
        return ResponseEntity.ok(ApiResponse.success(200, "Communication analytics overview fetched successfully", overview));
    }
}
