package com.mentify.service.registration;

import com.mentify.repository.RegistrationIdCounterRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MonthlyRegistrationIdGeneratorTest {

    private final RegistrationIdCounterRepository counterRepository = mock(RegistrationIdCounterRepository.class);
    private final MonthlyRegistrationIdGenerator generator = new MonthlyRegistrationIdGenerator(
            counterRepository,
            Clock.fixed(Instant.parse("2026-10-15T00:00:00Z"), ZoneId.of("Asia/Colombo")));

    @Test
    void generatesStudentIdFromYearMonthAndMonthlySequence() {
        when(counterRepository.nextNumber("STUDENT", 2026, 10)).thenReturn(34);

        assertThat(generator.generateStudentId()).isEqualTo("TIT-STU-202610-34");
    }

    @Test
    void generatesTeacherIdWithSeparateRolePrefix() {
        when(counterRepository.nextNumber("TEACHER", 2026, 10)).thenReturn(34);

        assertThat(generator.generateTeacherId()).isEqualTo("TIT-THR-202610-34");
    }
}
