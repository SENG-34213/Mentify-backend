package com.mentify.service.registration;

import com.mentify.repository.AdminProfileRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminCodeGeneratorTest {

    private final AdminProfileRepository adminProfileRepository = mock(AdminProfileRepository.class);
    private final AdminCodeGenerator adminCodeGenerator = new AdminCodeGenerator(adminProfileRepository);

    @Test
    void generate_usesNextGlobalAdminNumber() {
        when(adminProfileRepository.findLastAdminNumber()).thenReturn(12);

        String adminCode = adminCodeGenerator.generate();

        assertThat(adminCode).isEqualTo("TIT-ADM-013");
    }
}
