package com.mentify.mapper;

import com.mentify.dto.StudentRegistrationRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StudentMapperTest {

    private final StudentMapper studentMapper = new StudentMapper();

    @Test
    void mapsSchoolNameToStudentProfile() {
        StudentRegistrationRequest request = StudentRegistrationRequest.builder()
                .schoolName("  Mentify Academy  ")
                .build();

        assertThat(studentMapper.toStudentProfileEntity(request).getSchoolName())
                .isEqualTo("Mentify Academy");
    }
}
