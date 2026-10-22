package com.mentify.bootstrap;

import com.mentify.config.SuperAdminBootstrapProperties;
import com.mentify.entity.Address;
import com.mentify.entity.AdminProfile;
import com.mentify.entity.User;
import com.mentify.enums.AccountStatus;
import com.mentify.enums.Role;
import com.mentify.repository.UserRepository;
import com.mentify.service.authentication.AuthServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminBootstrap implements ApplicationRunner {

    private final SuperAdminBootstrapProperties properties;
    private final UserRepository userRepository;
    private final AuthServiceClient authServiceClient;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        if (userRepository.countByRole(Role.SUPER_ADMIN) > 0) {
            log.info("Super-admin bootstrap skipped because a SUPER_ADMIN already exists");
            return;
        }

        validateConfiguration();

        if (userRepository.existsByEmailIgnoreCase(properties.getEmail())) {
            throw new IllegalStateException("Super-admin bootstrap email already belongs to another user: "
                    + properties.getEmail());
        }

        UUID userId = UUID.randomUUID();
        String authUserId = userId.toString();

        User user = User.builder()
                .authUserId(authUserId)
                .email(properties.getEmail().trim().toLowerCase())
                .firstName(properties.getFirstName().trim())
                .lastName(properties.getLastName().trim())
                .phoneNumber(trimToNull(properties.getPhoneNumber()))
                .role(Role.SUPER_ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .accountNonLocked(true)
                .emailVerified(true)
                .loginAttempts(0)
                .emailNotifications(false)
                .pushNotifications(false)
                .dailyDigestEnabled(false)
                .build();

        user.setAdminProfile(AdminProfile.builder()
                .adminCode("SYS-ADM-001")
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .nic(trimToNull(properties.getNic()))
                .phoneNumber(user.getPhoneNumber())
                .build());

        user.setAddress(Address.builder()
                .addressLine1(properties.getAddressLine1().trim())
                .city(properties.getCity().trim())
                .district(properties.getDistrict().trim())
                .postalCode(trimToNull(properties.getPostalCode()))
                .build());

        User savedUser = userRepository.saveAndFlush(user);
        try {
            authServiceClient.provision(savedUser.getId(), savedUser.getEmail(), properties.getPassword());
        } catch (RuntimeException exception) {
            userRepository.delete(savedUser);
            throw new IllegalStateException("Unable to provision the super-admin credentials in auth-service", exception);
        }

        log.warn("Initial SUPER_ADMIN created for {}. Disable MENTIFY_SUPER_ADMIN_BOOTSTRAP_ENABLED after login.",
                savedUser.getEmail());
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(properties.getEmail()) || !StringUtils.hasText(properties.getPassword())) {
            throw new IllegalStateException(
                    "Super-admin bootstrap requires MENTIFY_SUPER_ADMIN_EMAIL and "
                            + "MENTIFY_SUPER_ADMIN_PASSWORD");
        }
        if (!StringUtils.hasText(properties.getFirstName()) || !StringUtils.hasText(properties.getLastName())
                || !StringUtils.hasText(properties.getAddressLine1()) || !StringUtils.hasText(properties.getCity())
                || !StringUtils.hasText(properties.getDistrict())) {
            throw new IllegalStateException("Super-admin bootstrap profile and address values are incomplete");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
