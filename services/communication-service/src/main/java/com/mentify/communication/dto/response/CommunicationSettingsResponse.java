package com.mentify.communication.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunicationSettingsResponse {

    private UUID userId;
    private boolean readReceiptsEnabled;
    private boolean lastSeenVisible;
    private boolean profilePhotoVisible;
    private boolean groupNotificationsEnabled;
    private boolean directNotificationsEnabled;
}
