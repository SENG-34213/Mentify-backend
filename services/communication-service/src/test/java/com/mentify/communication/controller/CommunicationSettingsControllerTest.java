package com.mentify.communication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.communication.dto.request.UpdateCommunicationSettingsRequest;
import com.mentify.communication.dto.response.CommunicationSettingsResponse;
import com.mentify.communication.service.CommunicationSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CommunicationSettingsController.class,
        properties = "spring.cloud.config.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
class CommunicationSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommunicationSettingsService communicationSettingsService;

    @Test
    void getSettingsReturnsCurrentUserSettings() throws Exception {
        UUID userId = UUID.randomUUID();
        when(communicationSettingsService.getCurrentUserSettings()).thenReturn(response(userId));

        mockMvc.perform(get("/api/communication/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Communication settings fetched successfully"))
                .andExpect(jsonPath("$.data.userId").value(userId.toString()));

        verify(communicationSettingsService).getCurrentUserSettings();
    }

    @Test
    void updateSettingsReturnsUpdatedValues() throws Exception {
        UUID userId = UUID.randomUUID();
        when(communicationSettingsService.updateCurrentUserSettings(any(UpdateCommunicationSettingsRequest.class)))
                .thenReturn(response(userId));

        mockMvc.perform(patch("/api/communication/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateCommunicationSettingsRequest.builder()
                                .readReceiptsEnabled(false)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Communication settings updated successfully"));

        verify(communicationSettingsService).updateCurrentUserSettings(any(UpdateCommunicationSettingsRequest.class));
    }

    private CommunicationSettingsResponse response(UUID userId) {
        return CommunicationSettingsResponse.builder()
                .userId(userId)
                .readReceiptsEnabled(true)
                .lastSeenVisible(true)
                .profilePhotoVisible(true)
                .groupNotificationsEnabled(true)
                .directNotificationsEnabled(true)
                .build();
    }
}
