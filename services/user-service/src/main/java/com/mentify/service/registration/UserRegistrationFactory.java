package com.mentify.service.registration;

import com.mentify.dto.AddressDto;
import com.mentify.dto.AdminRegisterUserRequest;
import com.mentify.entity.Address;
import com.mentify.entity.StudentProfile;
import com.mentify.entity.TeacherProfile;
import com.mentify.entity.User;
import com.mentify.enums.AccountStatus;
import com.mentify.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserRegistrationFactory {

    private final StudentIdGenerator studentIdGenerator;
    private final TeacherCodeGenerator teacherCodeGenerator;

    public User create(AdminRegisterUserRequest request, String keycloakUserId) {
        User user = buildUser(request, keycloakUserId);
        attachRoleProfile(user, request);
        attachAddress(user, request.getAddress());
        return user;
    }

    private User buildUser(AdminRegisterUserRequest request, String keycloakUserId) {
        return User.builder()
                .keycloakUserId(keycloakUserId)
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole())
                .accountStatus(AccountStatus.INVITED)
                .accountNonLocked(true)
                .emailVerified(false)
                .loginAttempts(0)
                .build();
    }

    private void attachRoleProfile(User user, AdminRegisterUserRequest request) {
        if (request.getRole() == Role.STUDENT) {
            attachStudentProfile(user, request);
            return;
        }

        attachTeacherProfile(user, request);
    }

    private void attachStudentProfile(User user, AdminRegisterUserRequest request) {
        AdminRegisterUserRequest.StudentProfileRequest profileRequest = request.getStudentProfile();
        String normalizedGrade = studentIdGenerator.normalizeGrade(profileRequest.getGrade());

        user.setStudentProfile(StudentProfile.builder()
                .studentId(studentIdGenerator.generate(normalizedGrade))
                .grade(normalizedGrade)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(profileRequest.getDateOfBirth())
                .phoneNumber(request.getPhoneNumber())
                .guardianName(profileRequest.getGuardianName())
                .guardianPhone(profileRequest.getGuardianPhone())
                .attendanceMode(profileRequest.getAttendanceMode())
                .build());
    }

    private void attachTeacherProfile(User user, AdminRegisterUserRequest request) {
        AdminRegisterUserRequest.TeacherProfileRequest profileRequest = request.getTeacherProfile();

        user.setTeacherProfile(TeacherProfile.builder()
                .teacherCode(teacherCodeGenerator.generate())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(profileRequest.getDateOfBirth())
                .phoneNumber(request.getPhoneNumber())
                .nic(profileRequest.getNic())
                .specialization(profileRequest.getSpecialization())
                .hireDate(profileRequest.getHireDate())
                .build());
    }

    private void attachAddress(User user, AddressDto addressDto) {
        user.setAddress(Address.builder()
                .addressLine1(addressDto.getAddressLine1())
                .addressLine2(addressDto.getAddressLine2())
                .city(addressDto.getCity())
                .district(addressDto.getDistrict())
                .postalCode(addressDto.getPostalCode())
                .build());
    }
}
