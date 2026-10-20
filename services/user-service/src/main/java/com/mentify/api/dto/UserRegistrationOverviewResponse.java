package com.mentify.dto;

import com.mentify.entity.User;
import com.mentify.enums.AccountStatus;
import com.mentify.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationOverviewResponse {

    private long totalUsers;
    private Map<Role, Long> usersByRole;
    private Map<AccountStatus, Long> usersByAccountStatus;
    private List<RegisteredUserSummary> recentStudents;
    private List<RegisteredUserSummary> recentTeachers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisteredUserSummary {
        private UUID id;
        private String keycloakUserId;
        private String email;
        private String firstName;
        private String lastName;
        private Role role;
        private AccountStatus accountStatus;
        private LocalDateTime createdAt;
        private String studentId;
        private String grade;
        private String teacherCode;
        private List<String> specializations;

        public static RegisteredUserSummary from(User user) {
            return RegisteredUserSummary.builder()
                    .id(user.getId())
                    .keycloakUserId(user.getKeycloakUserId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .role(user.getRole())
                    .accountStatus(user.getAccountStatus())
                    .createdAt(user.getCreatedAt())
                    .studentId(user.getStudentProfile() != null ? user.getStudentProfile().getStudentId() : null)
                    .grade(user.getStudentProfile() != null ? user.getStudentProfile().getGrade() : null)
                    .teacherCode(user.getTeacherProfile() != null ? user.getTeacherProfile().getTeacherCode() : null)
                    .specializations(user.getTeacherProfile() != null ? user.getTeacherProfile().getSpecializations() : null)
                    .build();
        }
    }
}
