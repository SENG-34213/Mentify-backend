package com.mentify.ai.dto.tool;

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
public class UserRegistrationOverviewToolResult {

    private long totalUsers;
    private Map<String, Long> usersByRole;
    private Map<String, Long> usersByAccountStatus;
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
        private String role;
        private String accountStatus;
        private LocalDateTime createdAt;
        private String studentId;
        private String grade;
        private String teacherCode;
        private List<String> specializations;
    }
}
