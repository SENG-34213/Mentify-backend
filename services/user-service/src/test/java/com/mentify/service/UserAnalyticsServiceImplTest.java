package com.mentify.service;

import com.mentify.dto.UserRegistrationOverviewResponse;
import com.mentify.entity.StudentProfile;
import com.mentify.entity.TeacherProfile;
import com.mentify.entity.User;
import com.mentify.enums.AccountStatus;
import com.mentify.enums.Role;
import com.mentify.repository.UserRepository;
import com.mentify.service.impl.UserAnalyticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAnalyticsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserAnalyticsServiceImpl userAnalyticsService;

    @BeforeEach
    void setUp() {
        userAnalyticsService = new UserAnalyticsServiceImpl(userRepository);
    }

    @Test
    void registrationOverviewReturnsCountsAndRecentStudentsTeachers() {
        User student = studentUser("student@example.com", "TIT-03-001", "03");
        User teacher = teacherUser("teacher@example.com", "TIT-TCH-001", List.of("Math"));

        when(userRepository.count()).thenReturn(3L);
        when(userRepository.countByRole(Role.STUDENT)).thenReturn(1L);
        when(userRepository.countByRole(Role.TEACHER)).thenReturn(1L);
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);
        when(userRepository.countByRole(Role.SUPER_ADMIN)).thenReturn(0L);
        when(userRepository.countByAccountStatus(AccountStatus.ACTIVE)).thenReturn(2L);
        when(userRepository.countByAccountStatus(AccountStatus.INVITED)).thenReturn(1L);
        when(userRepository.countByAccountStatus(AccountStatus.SUSPENDED)).thenReturn(0L);
        when(userRepository.countByAccountStatus(AccountStatus.DISABLED)).thenReturn(0L);
        when(userRepository.findTop5ByRoleOrderByCreatedAtDesc(Role.STUDENT)).thenReturn(List.of(student));
        when(userRepository.findTop5ByRoleOrderByCreatedAtDesc(Role.TEACHER)).thenReturn(List.of(teacher));

        UserRegistrationOverviewResponse response = userAnalyticsService.getRegistrationOverview();

        assertThat(response.getTotalUsers()).isEqualTo(3L);
        assertThat(response.getUsersByRole()).containsEntry(Role.STUDENT, 1L);
        assertThat(response.getUsersByRole()).containsEntry(Role.TEACHER, 1L);
        assertThat(response.getUsersByAccountStatus()).containsEntry(AccountStatus.ACTIVE, 2L);
        assertThat(response.getRecentStudents()).hasSize(1);
        assertThat(response.getRecentStudents().get(0).getStudentId()).isEqualTo("TIT-03-001");
        assertThat(response.getRecentStudents().get(0).getGrade()).isEqualTo("03");
        assertThat(response.getRecentTeachers()).hasSize(1);
        assertThat(response.getRecentTeachers().get(0).getTeacherCode()).isEqualTo("TIT-TCH-001");
        assertThat(response.getRecentTeachers().get(0).getSpecializations()).containsExactly("Math");
    }

    private User studentUser(String email, String studentId, String grade) {
        User user = baseUser(email, Role.STUDENT);
        StudentProfile profile = StudentProfile.builder()
                .studentId(studentId)
                .grade(grade)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
        user.setStudentProfile(profile);
        return user;
    }

    private User teacherUser(String email, String teacherCode, List<String> specializations) {
        User user = baseUser(email, Role.TEACHER);
        TeacherProfile profile = TeacherProfile.builder()
                .teacherCode(teacherCode)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .specializations(specializations)
                .build();
        user.setTeacherProfile(profile);
        return user;
    }

    private User baseUser(String email, Role role) {
        User user = User.builder()
                .keycloakUserId(UUID.randomUUID().toString())
                .email(email)
                .firstName("Test")
                .lastName("User")
                .role(role)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        user.setId(UUID.randomUUID());
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}
