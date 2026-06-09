package com.mentify.service.registration;

import com.mentify.dto.AdminRegisterUserRequest;
import com.mentify.enums.Role;
import com.mentify.exception.InvalidRoleException;
import org.springframework.stereotype.Component;

@Component
public class RegistrationRequestValidator {

    public void validate(AdminRegisterUserRequest request) {
        if (request.getRole() != Role.STUDENT && request.getRole() != Role.TEACHER) {
            throw new InvalidRoleException("Only STUDENT and TEACHER users can be registered from this endpoint");
        }

        if (request.getAddress() == null) {
            throw new InvalidRoleException("Address details are required when registering a user");
        }

        if (request.getRole() == Role.STUDENT) {
            validateStudentRequest(request);
            return;
        }

        validateTeacherRequest(request);
    }

    private void validateStudentRequest(AdminRegisterUserRequest request) {
        if (request.getStudentProfile() == null) {
            throw new InvalidRoleException("Student profile details are required when registering a STUDENT");
        }
        if (request.getTeacherProfile() != null) {
            throw new InvalidRoleException("Teacher profile details are not allowed when registering a STUDENT");
        }
    }

    private void validateTeacherRequest(AdminRegisterUserRequest request) {
        if (request.getTeacherProfile() == null) {
            throw new InvalidRoleException("Teacher profile details are required when registering a TEACHER");
        }
        if (request.getStudentProfile() != null) {
            throw new InvalidRoleException("Student profile details are not allowed when registering a TEACHER");
        }
    }
}
