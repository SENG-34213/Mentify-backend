package com.mentify.communication.service.impl;

import com.mentify.communication.dto.request.UpdateCommunicationSettingsRequest;
import com.mentify.communication.dto.response.CommunicationSettingsResponse;
import com.mentify.communication.entity.CommunicationSettings;
import com.mentify.communication.repository.CommunicationSettingsRepository;
import com.mentify.communication.security.AuthenticatedUserService;
import com.mentify.communication.service.CommunicationSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunicationSettingsServiceImpl implements CommunicationSettingsService {

    private final CommunicationSettingsRepository communicationSettingsRepository;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    @Transactional
    public CommunicationSettingsResponse getCurrentUserSettings() {
        UUID userId = authenticatedUserService.getCurrentUserId();
        return toResponse(getOrCreate(userId));
    }

    @Override
    @Transactional
    public CommunicationSettingsResponse updateCurrentUserSettings(UpdateCommunicationSettingsRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Settings request is required");
        }

        UUID userId = authenticatedUserService.getCurrentUserId();
        CommunicationSettings settings = getOrCreate(userId);

        if (request.getReadReceiptsEnabled() != null) {
            settings.setReadReceiptsEnabled(request.getReadReceiptsEnabled());
        }
        if (request.getLastSeenVisible() != null) {
            settings.setLastSeenVisible(request.getLastSeenVisible());
        }
        if (request.getProfilePhotoVisible() != null) {
            settings.setProfilePhotoVisible(request.getProfilePhotoVisible());
        }
        if (request.getGroupNotificationsEnabled() != null) {
            settings.setGroupNotificationsEnabled(request.getGroupNotificationsEnabled());
        }
        if (request.getDirectNotificationsEnabled() != null) {
            settings.setDirectNotificationsEnabled(request.getDirectNotificationsEnabled());
        }
        settings.setUpdatedBy(userId);

        return toResponse(communicationSettingsRepository.save(settings));
    }

    private CommunicationSettings getOrCreate(UUID userId) {
        return communicationSettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    CommunicationSettings settings = CommunicationSettings.builder()
                            .userId(userId)
                            .build();
                    settings.setCreatedBy(userId);
                    settings.setUpdatedBy(userId);
                    settings.setActive(true);
                    return communicationSettingsRepository.save(settings);
                });
    }

    private CommunicationSettingsResponse toResponse(CommunicationSettings settings) {
        return CommunicationSettingsResponse.builder()
                .userId(settings.getUserId())
                .readReceiptsEnabled(settings.isReadReceiptsEnabled())
                .lastSeenVisible(settings.isLastSeenVisible())
                .profilePhotoVisible(settings.isProfilePhotoVisible())
                .groupNotificationsEnabled(settings.isGroupNotificationsEnabled())
                .directNotificationsEnabled(settings.isDirectNotificationsEnabled())
                .build();
    }
}
