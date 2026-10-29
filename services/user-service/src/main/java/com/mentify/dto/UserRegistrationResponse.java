package com.mentify.dto;

import com.mentify.entity.Address;
import com.mentify.entity.AdminProfile;
import com.mentify.entity.StudentProfile;
import com.mentify.entity.TeacherProfile;
import com.mentify.entity.User;
import com.mentify.enums.AccountStatus;
import com.mentify.enums.AttendanceMode;
import com.mentify.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationResponse {

    private UUID id;
    private String authUserId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Role role;
    private AccountStatus accountStatus;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private boolean emailNotifications;
    private boolean pushNotifications;
    private boolean dailyDigestEnabled;
    private java.time.LocalTime dailyDigestTime;
    private StudentProfileResponse studentProfile;
    private TeacherProfileResponse teacherProfile;
    private AdminProfileResponse adminProfile;
    private AddressResponse address;
    private boolean accountLocked;

    public UserRegistrationResponse(UUID id, String authUserId, String email,
                                    String firstName, String lastName, String phoneNumber,
                                    Role role, AccountStatus accountStatus,
                                    StudentProfileResponse studentProfile,
                                    TeacherProfileResponse teacherProfile,
                                    AdminProfileResponse adminProfile,
                                    AddressResponse address) {
        this(id, authUserId, email, firstName, lastName, phoneNumber, role, accountStatus,
                null, null, true, true, false, null,
                studentProfile, teacherProfile, adminProfile, address, false);
    }

    public static UserRegistrationResponse from(User user) {
        return UserRegistrationResponse.builder()
                .id(user.getId())
                .authUserId(user.getAuthUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .accountStatus(user.getAccountStatus())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .emailNotifications(user.isEmailNotifications())
                .pushNotifications(user.isPushNotifications())
                .dailyDigestEnabled(user.isDailyDigestEnabled())
                .dailyDigestTime(user.getDailyDigestTime())
                .studentProfile(StudentProfileResponse.from(user.getStudentProfile()))
                .teacherProfile(TeacherProfileResponse.from(user.getTeacherProfile()))
                .adminProfile(AdminProfileResponse.from(user.getAdminProfile()))
                .address(AddressResponse.from(user.getAddress()))
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentProfileResponse {
        private UUID id;
        private String studentId;
        private String grade;
        private String schoolName;
        private String bio;
        private String avatarUrl;
        private LocalDate dateOfBirth;
        private String guardianName;
        private String guardianPhone;
        private AttendanceMode attendanceMode;

        public static StudentProfileResponse from(StudentProfile profile) {
            if (profile == null) {
                return null;
            }

            return StudentProfileResponse.builder()
                    .id(profile.getId())
                    .studentId(profile.getStudentId())
                    .grade(profile.getGrade())
                    .schoolName(profile.getSchoolName())
                    .bio(profile.getBiography())
                    .avatarUrl(profile.getAvatarUrl())
                    .dateOfBirth(profile.getDateOfBirth())
                    .guardianName(profile.getGuardianName())
                    .guardianPhone(profile.getGuardianPhone())
                    .attendanceMode(profile.getAttendanceMode())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherProfileResponse {
        private UUID id;
        private String teacherCode;
        private LocalDate dateOfBirth;
        private String nic;
        private List<String> specializations;
        private String title;
        private String department;
        private String officeLocation;
        private String officeHours;
        private String bio;
        private String avatarUrl;
        private LocalDate hireDate;

        public static TeacherProfileResponse from(TeacherProfile profile) {
            if (profile == null) {
                return null;
            }

            return TeacherProfileResponse.builder()
                    .id(profile.getId())
                    .teacherCode(profile.getTeacherCode())
                    .dateOfBirth(profile.getDateOfBirth())
                    .nic(profile.getNic())
                    .specializations(profile.getSpecializations())
                    .title(profile.getAcademicTitle())
                    .department(profile.getDepartment())
                    .officeLocation(profile.getOfficeLocation())
                    .officeHours(profile.getOfficeHours())
                    .bio(profile.getBiography())
                    .avatarUrl(profile.getAvatarUrl())
                    .hireDate(profile.getHireDate())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminProfileResponse {
        private UUID id;
        private String adminCode;
        private LocalDate dateOfBirth;
        private String nic;

        public static AdminProfileResponse from(AdminProfile profile) {
            if (profile == null) {
                return null;
            }

            return AdminProfileResponse.builder()
                    .id(profile.getId())
                    .adminCode(profile.getAdminCode())
                    .dateOfBirth(profile.getDateOfBirth())
                    .nic(profile.getNic())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressResponse {
        private UUID id;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String district;
        private String postalCode;

        public static AddressResponse from(Address address) {
            if (address == null) {
                return null;
            }

            return AddressResponse.builder()
                    .id(address.getId())
                    .addressLine1(address.getAddressLine1())
                    .addressLine2(address.getAddressLine2())
                    .city(address.getCity())
                    .district(address.getDistrict())
                    .postalCode(address.getPostalCode())
                    .build();
        }
    }
}
