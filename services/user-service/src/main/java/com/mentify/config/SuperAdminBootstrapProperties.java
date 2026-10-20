package com.mentify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mentify.super-admin-bootstrap")
public class SuperAdminBootstrapProperties {

    private boolean enabled = false;
    private String email;
    private String password;
    private String firstName = "System";
    private String lastName = "Administrator";
    private String phoneNumber;
    private String addressLine1 = "System address";
    private String city = "Colombo";
    private String district = "Colombo";
    private String postalCode;
    private String nic;
}
