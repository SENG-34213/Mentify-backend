package com.mentify.communication.service;

import com.mentify.communication.TestJwtDecoderConfig;
import com.mentify.communication.client.CourseServiceClient;
import com.mentify.communication.client.EntrollmentServiceClient;
import com.mentify.communication.dto.request.UpdateCommunicationSettingsRequest;
import com.mentify.communication.dto.response.CommunicationSettingsResponse;
import com.mentify.communication.entity.CommunicationSettings;
import com.mentify.communication.repository.CommunicationSettingsRepository;
import com.mentify.communication.security.AuthenticatedUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:communication_settings_it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "mentify.security.enabled=true"
})
@Import(TestJwtDecoderConfig.class)
@Transactional
class CommunicationSettingsServiceIntegrationTest {

    @Autowired
    private CommunicationSettingsService communicationSettingsService;

    @Autowired
    private CommunicationSettingsRepository communicationSettingsRepository;

    @MockBean
    private AuthenticatedUserService authenticatedUserService;

    @MockBean
    private CourseServiceClient courseServiceClient;

    @MockBean
    private EntrollmentServiceClient entrollmentServiceClient;

    @Test
    void createDefaultSettingsOnFirstAccess() {
        UUID userId = UUID.randomUUID();
        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);

        CommunicationSettingsResponse response = communicationSettingsService.getCurrentUserSettings();

        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.isReadReceiptsEnabled()).isTrue();
        assertThat(communicationSettingsRepository.findByUserId(userId)).isPresent();
    }

    @Test
    void updateSettingsPersistsValues() {
        UUID userId = UUID.randomUUID();
        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);

        CommunicationSettingsResponse response = communicationSettingsService.updateCurrentUserSettings(
                UpdateCommunicationSettingsRequest.builder()
                        .readReceiptsEnabled(false)
                        .lastSeenVisible(false)
                        .build()
        );

        assertThat(response.isReadReceiptsEnabled()).isFalse();
        assertThat(response.isLastSeenVisible()).isFalse();

        CommunicationSettings persisted = communicationSettingsRepository.findByUserId(userId).orElseThrow();
        assertThat(persisted.isReadReceiptsEnabled()).isFalse();
        assertThat(persisted.isLastSeenVisible()).isFalse();
    }
}
