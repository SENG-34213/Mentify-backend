package com.mentify.communication.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCommunicationSettingsRequest {

    private Boolean readReceiptsEnabled;
    private Boolean lastSeenVisible;
    private Boolean profilePhotoVisible;
    private Boolean groupNotificationsEnabled;
    private Boolean directNotificationsEnabled;
}
