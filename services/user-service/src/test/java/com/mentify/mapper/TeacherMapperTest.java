package com.mentify.mapper;

import com.mentify.entity.TeacherProfile;
import com.mentify.entity.User;
import com.mentify.enums.Role;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TeacherMapperTest {
    @Test
    void mapsTeacherIdentityAndStaffMetadata() {
        User user = User.builder().firstName("Ada").lastName("Lovelace")
                .email("ada@example.com").role(Role.TEACHER).build();
        user.setTeacherProfile(TeacherProfile.builder().teacherCode("T-001")
                .academicTitle("Senior Lecturer").department("Computing")
                .officeLocation("B-204").officeHours("Mon 10:00-12:00")
                .specializations(List.of("Algorithms", "AI")).build());

        var response = new TeacherMapper().toUserResponse(user);

        assertThat(response.getFullName()).isEqualTo("Ada Lovelace");
        assertThat(response.getEmail()).isEqualTo("ada@example.com");
        assertThat(response.getTeacherCode()).isEqualTo("T-001");
        assertThat(response.getTitle()).isEqualTo("Senior Lecturer");
        assertThat(response.getDepartment()).isEqualTo("Computing");
        assertThat(response.getStaffMetadata()).contains("Algorithms", "AI");
    }
}
