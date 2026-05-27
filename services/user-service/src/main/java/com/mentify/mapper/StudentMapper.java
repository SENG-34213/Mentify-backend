package com.mentify.mapper;

import com.mentify.dto.AddressDto;
import com.mentify.dto.StudentRegistrationRequest;
import com.mentify.dto.UserResponse;
import com.mentify.entity.Address;
import com.mentify.entity.StudentProfile;
import com.mentify.entity.User;
import com.mentify.enums.AccountStatus;
import com.mentify.enums.Role;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StudentMapper {

    public User toUserEntity(StudentRegistrationRequest request) {
        return User.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .role(Role.STUDENT)
                .accountStatus(AccountStatus.ACTIVE)
                .accountNonLocked(true)
                .emailVerified(false)
                .loginAttempts(0)
                .build();
    }

    public StudentProfile toStudentProfileEntity(StudentRegistrationRequest request) {
        return StudentProfile.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .phoneNumber(request.getPhoneNumber())
                .guardianName(request.getGuardianName())
                .guardianPhone(request.getGuardianPhone())
                .attendanceMode(request.getAttendanceMode())
                .admissionId("STU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .build();
    }

    public Address toAddressEntity(AddressDto addressDto) {
        if (addressDto == null) {
            return null;
        }
        return Address.builder()
                .addressLine1(addressDto.getAddressLine1())
                .addressLine2(addressDto.getAddressLine2())
                .city(addressDto.getCity())
                .district(addressDto.getDistrict())
                .postalCode(addressDto.getPostalCode())
                .build();
    }

    public UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .accountStatus(user.getAccountStatus())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
