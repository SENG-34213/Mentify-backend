package com.mentify.dto;

import com.mentify.entity.Address;
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
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationResponse {

    private UUID id;
    private String keycloakUserId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Role role;
    private AccountStatus accountStatus;
    private StudentProfileResponse studentProfile;
    private TeacherProfileResponse teacherProfile;
    private AddressResponse address;

    public static UserRegistrationResponse from(User user) {
        return UserRegistrationResponse.builder()
                .id(user.getId())
                .keycloakUserId(user.getKeycloakUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .accountStatus(user.getAccountStatus())
                .studentProfile(StudentProfileResponse.from(user.getStudentProfile()))
                .teacherProfile(TeacherProfileResponse.from(user.getTeacherProfile()))
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
        private String specialization;
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
                    .specialization(profile.getSpecialization())
                    .hireDate(profile.getHireDate())
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
