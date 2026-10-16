package com.mentify.communication.service.impl;

import com.mentify.communication.dto.request.UpdateCommunicationSettingsRequest;
import com.mentify.communication.dto.response.CommunicationSettingsResponse;
import com.mentify.communication.entity.CommunicationSettings;
import com.mentify.communication.repository.CommunicationSettingsRepository;
import com.mentify.communication.security.AuthenticatedUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunicationSettingsServiceImplTest {

    @Mock
    private CommunicationSettingsRepository communicationSettingsRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @InjectMocks
    private CommunicationSettingsServiceImpl communicationSettingsService;

    @Test
    void returnsDefaultSettingsWhenNotConfigured() {
        UUID userId = UUID.randomUUID();
        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(communicationSettingsRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(communicationSettingsRepository.save(any(CommunicationSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommunicationSettingsResponse response = communicationSettingsService.getCurrentUserSettings();

        assertEquals(userId, response.getUserId());
        assertTrue(response.isReadReceiptsEnabled());
        assertTrue(response.isGroupNotificationsEnabled());
    }

    @Test
    void updatesOnlyProvidedFlags() {
        UUID userId = UUID.randomUUID();
        CommunicationSettings settings = CommunicationSettings.builder().userId(userId).build();
        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(communicationSettingsRepository.findByUserId(userId)).thenReturn(Optional.of(settings));
        when(communicationSettingsRepository.save(any(CommunicationSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommunicationSettingsResponse response = communicationSettingsService.updateCurrentUserSettings(
                UpdateCommunicationSettingsRequest.builder()
                        .readReceiptsEnabled(false)
                        .groupNotificationsEnabled(false)
                        .build()
        );

        assertFalse(response.isReadReceiptsEnabled());
        assertFalse(response.isGroupNotificationsEnabled());
        assertTrue(response.isDirectNotificationsEnabled());
    }

    @Test
    void nullUpdateRequestIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> communicationSettingsService.updateCurrentUserSettings(null));
    }
}
