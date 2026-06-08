package com.mentify.dto;

import com.mentify.enums.AttendanceMode;
import com.mentify.enums.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRegisterUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Pattern(regexp = "^(?:\\+94|0)[7][0-9]{8}$", message = "Invalid Sri Lankan phone number")
    private String phoneNumber;

    @NotNull(message = "Role is required")
    private Role role;

    @Valid
    private StudentProfileRequest studentProfile;

    @Valid
    private TeacherProfileRequest teacherProfile;

    @Valid
    @NotNull(message = "Address details are required")
    private AddressDto address;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentProfileRequest {

        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        private LocalDate dateOfBirth;

        @NotBlank(message = "Guardian name is required")
        private String guardianName;

        @NotBlank(message = "Guardian phone is required")
        @Pattern(regexp = "^(?:\\+94|0)[7][0-9]{8}$", message = "Invalid Sri Lankan phone number")
        private String guardianPhone;

        @NotNull(message = "Attendance mode is required")
        private AttendanceMode attendanceMode;

        private String gradeGroupChatId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherProfileRequest {

        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        private LocalDate dateOfBirth;

        private String nic;

        private String specialization;

        private LocalDate hireDate;
    }
}
