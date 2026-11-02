package com.mentify.service;

import com.mentify.dto.PreRegistrationUpdateRequest;
import com.mentify.entity.PreRegistration;
import com.mentify.enums.PreRegistrationStatus;
import com.mentify.repository.PreRegistrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreRegistrationServiceTest {
    @Mock
    private PreRegistrationRepository repository;

    @InjectMocks
    private PreRegistrationService service;

    @Test
    void updatesWorkflowAndRemarkWithoutChangingSeenState() {
        UUID id = UUID.randomUUID();
        PreRegistration registration = PreRegistration.builder()
                .id(id)
                .name("Sample Applicant")
                .email("sample@example.com")
                .phone("0771234567")
                .role("student")
                .district("Jaffna")
                .city("Jaffna")
                .classFormat("online")
                .grade("al")
                .contactMethod("whatsapp")
                .consent(true)
                .status(PreRegistrationStatus.NEW)
                .seen(false)
                .build();
        PreRegistrationUpdateRequest request = new PreRegistrationUpdateRequest();
        request.setStatus(PreRegistrationStatus.COMPLETED);
        request.setAdminRemark("  Contacted the guardian.  ");

        when(repository.findById(id)).thenReturn(Optional.of(registration));
        when(repository.save(any(PreRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.update(id, request);

        assertEquals(PreRegistrationStatus.COMPLETED, response.status());
        assertFalse(response.seen());
        assertEquals("Contacted the guardian.", response.adminRemark());
        verify(repository).save(registration);
    }
}