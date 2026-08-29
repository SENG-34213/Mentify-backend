package com.mentify.service.registration;

import com.mentify.repository.TeacherProfileRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TeacherCodeGeneratorTest {

    private final TeacherProfileRepository teacherProfileRepository = mock(TeacherProfileRepository.class);
    private final TeacherCodeGenerator teacherCodeGenerator = new TeacherCodeGenerator(teacherProfileRepository);

    @Test
    void generate_usesNextGlobalTeacherNumber() {
        when(teacherProfileRepository.findLastTeacherNumber()).thenReturn(12);

        String teacherCode = teacherCodeGenerator.generate();

        assertThat(teacherCode).isEqualTo("TIT-TCH-013");
    }
}
