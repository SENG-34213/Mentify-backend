package com.mentify.communication.service;

import com.mentify.communication.dto.request.UpdateCommunicationSettingsRequest;
import com.mentify.communication.dto.response.CommunicationSettingsResponse;

public interface CommunicationSettingsService {

    CommunicationSettingsResponse getCurrentUserSettings();

    CommunicationSettingsResponse updateCurrentUserSettings(UpdateCommunicationSettingsRequest request);
}
