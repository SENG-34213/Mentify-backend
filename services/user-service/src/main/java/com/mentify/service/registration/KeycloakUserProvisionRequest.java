package com.mentify.service.registration;

import com.mentify.dto.AdminRegisterUserRequest;
import com.mentify.dto.SuperAdminRegisterAdminRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class KeycloakUserProvisionRequest {

    private String email;
    private String firstName;
    private String lastName;

    public static KeycloakUserProvisionRequest from(AdminRegisterUserRequest request) {
        return new KeycloakUserProvisionRequest(
                request.getEmail(),
                request.getFirstName(),
                request.getLastName()
        );
    }

    public static KeycloakUserProvisionRequest from(SuperAdminRegisterAdminRequest request) {
        return new KeycloakUserProvisionRequest(
                request.getEmail(),
                request.getFirstName(),
                request.getLastName()
        );
    }
}
