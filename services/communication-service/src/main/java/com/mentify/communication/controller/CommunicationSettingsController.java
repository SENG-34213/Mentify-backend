package com.mentify.communication.controller;

import com.mentify.communication.dto.request.UpdateCommunicationSettingsRequest;
import com.mentify.communication.dto.response.CommunicationSettingsResponse;
import com.mentify.communication.service.CommunicationSettingsService;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/communication/settings")
@RequiredArgsConstructor
public class CommunicationSettingsController {

    private final CommunicationSettingsService communicationSettingsService;

    @GetMapping
    public ResponseEntity<ApiResponse<CommunicationSettingsResponse>> getSettings() {
        CommunicationSettingsResponse response = communicationSettingsService.getCurrentUserSettings();

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Communication settings fetched successfully", response));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<CommunicationSettingsResponse>> updateSettings(
            @RequestBody UpdateCommunicationSettingsRequest request
    ) {
        CommunicationSettingsResponse response = communicationSettingsService.updateCurrentUserSettings(request);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Communication settings updated successfully", response));
    }
}
