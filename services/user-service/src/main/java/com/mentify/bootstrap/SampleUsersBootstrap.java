package com.mentify.bootstrap;

import com.mentify.config.KeycloakProperties;
import com.mentify.config.SampleUsersProperties;
import com.mentify.entity.Address;
import com.mentify.entity.AdminProfile;
import com.mentify.entity.StudentProfile;
import com.mentify.entity.TeacherProfile;
import com.mentify.entity.User;
import com.mentify.enums.AccountStatus;
import com.mentify.enums.AttendanceMode;
import com.mentify.enums.Role;
import com.mentify.repository.UserRepository;
import com.mentify.service.registration.AdminCodeGenerator;
import com.mentify.service.registration.StudentIdGenerator;
import com.mentify.service.registration.TeacherCodeGenerator;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SampleUsersBootstrap implements ApplicationRunner {

    private static final String PASSWORD_CREDENTIAL_TYPE = "password";

    private final Keycloak keycloak;
    private final KeycloakProperties keycloakProperties;
    private final SampleUsersProperties sampleUsersProperties;
    private final UserRepository userRepository;
    private final StudentIdGenerator studentIdGenerator;
    private final TeacherCodeGenerator teacherCodeGenerator;
    private final AdminCodeGenerator adminCodeGenerator;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!sampleUsersProperties.isEnabled()) {
            return;
        }

        validate();
        bootstrapWithRetry();
    }

    void bootstrapOnce() {
        RealmResource realm = keycloak.realm(keycloakProperties.getRealm());
        UsersResource users = realm.users();

        for (Role role : List.of(Role.STUDENT, Role.TEACHER, Role.ADMIN)) {
            ensureRealmRoleExists(realm, role);
            for (int index = 1; index <= sampleUsersProperties.getUsersPerRole(); index++) {
                bootstrapSampleUser(realm, users, role, index);
            }
        }

        log.info(
                "Sample login users bootstrap completed: {} users per STUDENT, TEACHER, ADMIN role",
                sampleUsersProperties.getUsersPerRole()
        );
    }

    private void bootstrapWithRetry() {
        int attempts = Math.max(1, sampleUsersProperties.getRetryAttempts());

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                bootstrapOnce();
                return;
            } catch (NotAuthorizedException | ForbiddenException exception) {
                throw new IllegalStateException(
                        "Sample users bootstrap is not authorized. Verify the "
                                + keycloakProperties.getAdminClientId()
                                + " service account has realm-management roles: manage-realm, view-realm, "
                                + "manage-users, view-users, query-users.",
                        exception
                );
            } catch (ProcessingException | WebApplicationException | IllegalStateException exception) {
                if (attempt == attempts) {
                    throw exception;
                }

                log.warn(
                        "Sample users bootstrap attempt {}/{} failed. Retrying in {} ms",
                        attempt,
                        attempts,
                        sampleUsersProperties.getRetryDelayMillis(),
                        exception
                );
                sleep(sampleUsersProperties.getRetryDelayMillis());
            }
        }
    }

    private void bootstrapSampleUser(RealmResource realm, UsersResource users, Role role, int index) {
        SampleUser sampleUser = sampleUser(role, index);
        String keycloakUserId = ensureKeycloakUser(users, sampleUser);
        assignRealmRoleIfMissing(realm, users, keycloakUserId, role);

        transactionTemplate.executeWithoutResult(status -> ensureLocalUser(sampleUser, keycloakUserId));
    }

    private String ensureKeycloakUser(UsersResource users, SampleUser sampleUser) {
        Optional<UserRepresentation> existingUser = findUserByEmail(users, sampleUser.email());
        if (existingUser.isPresent()) {
            UserRepresentation userRepresentation = existingUser.get();
            String keycloakUserId = userRepresentation.getId();
            updateKeycloakUser(users, keycloakUserId, userRepresentation, sampleUser);

            if (sampleUsersProperties.isResetPasswordOnStartup()) {
                setPermanentPassword(users, keycloakUserId);
            }
            return keycloakUserId;
        }

        return createKeycloakUser(users, sampleUser);
    }

    private void updateKeycloakUser(
            UsersResource users,
            String keycloakUserId,
            UserRepresentation userRepresentation,
            SampleUser sampleUser
    ) {
        UserResource userResource = users.get(keycloakUserId);
        UserRepresentation currentRepresentation = userResource.toRepresentation();
        currentRepresentation.setUsername(sampleUser.email());
        currentRepresentation.setEmail(sampleUser.email());
        currentRepresentation.setFirstName(sampleUser.firstName());
        currentRepresentation.setLastName(sampleUser.lastName());
        currentRepresentation.setEnabled(true);
        currentRepresentation.setEmailVerified(true);
        currentRepresentation.setRequiredActions(List.of());
        userResource.update(currentRepresentation);
    }

    private String createKeycloakUser(UsersResource users, SampleUser sampleUser) {
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(sampleUser.email());
        userRepresentation.setEmail(sampleUser.email());
        userRepresentation.setFirstName(sampleUser.firstName());
        userRepresentation.setLastName(sampleUser.lastName());
        userRepresentation.setEnabled(true);
        userRepresentation.setEmailVerified(true);
        userRepresentation.setRequiredActions(List.of());

        try (Response response = users.create(userRepresentation)) {
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                throw new IllegalStateException(
                        "Failed to create sample Keycloak user " + sampleUser.email()
                                + ". Status: " + response.getStatus()
                );
            }

            String keycloakUserId = extractCreatedUserId(response.getLocation(), sampleUser.email());
            setPermanentPassword(users, keycloakUserId);
            return keycloakUserId;
        } catch (ProcessingException | WebApplicationException exception) {
            throw new IllegalStateException("Failed to create sample Keycloak user " + sampleUser.email(), exception);
        }
    }

    private void ensureLocalUser(SampleUser sampleUser, String keycloakUserId) {
        User user = userRepository.findByEmailIgnoreCase(sampleUser.email())
                .orElseGet(() -> buildLocalUser(sampleUser, keycloakUserId));

        user.setKeycloakUserId(keycloakUserId);
        user.setEmail(sampleUser.email());
        user.setFirstName(sampleUser.firstName());
        user.setLastName(sampleUser.lastName());
        user.setPhoneNumber(sampleUser.phoneNumber());
        user.setRole(sampleUser.role());
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setAccountNonLocked(true);
        user.setLoginAttempts(0);
        user.setEmailVerified(true);
        user.setActive(true);

        attachMissingProfile(user, sampleUser);
        attachAddressIfMissing(user);

        userRepository.saveAndFlush(user);
    }

    private User buildLocalUser(SampleUser sampleUser, String keycloakUserId) {
        return User.builder()
                .keycloakUserId(keycloakUserId)
                .email(sampleUser.email())
                .firstName(sampleUser.firstName())
                .lastName(sampleUser.lastName())
                .phoneNumber(sampleUser.phoneNumber())
                .role(sampleUser.role())
                .accountStatus(AccountStatus.ACTIVE)
                .accountNonLocked(true)
                .emailVerified(true)
                .loginAttempts(0)
                .build();
    }

    private void attachMissingProfile(User user, SampleUser sampleUser) {
        if (sampleUser.role() == Role.STUDENT && user.getStudentProfile() == null) {
            String grade = studentIdGenerator.normalizeGrade(String.valueOf(6 + sampleUser.index()));
            user.setStudentProfile(StudentProfile.builder()
                    .studentId(studentIdGenerator.generate(grade))
                    .grade(grade)
                    .firstName(sampleUser.firstName())
                    .lastName(sampleUser.lastName())
                    .dateOfBirth(LocalDate.of(2010, sampleUser.index(), 10))
                    .phoneNumber(sampleUser.phoneNumber())
                    .guardianName("Sample Guardian " + sampleUser.index())
                    .guardianPhone(samplePhoneNumber(sampleUser.index() + 10))
                    .attendanceMode(AttendanceMode.PHYSICAL)
                    .build());
            return;
        }

        if (sampleUser.role() == Role.TEACHER && user.getTeacherProfile() == null) {
            user.setTeacherProfile(TeacherProfile.builder()
                    .teacherCode(teacherCodeGenerator.generate())
                    .firstName(sampleUser.firstName())
                    .lastName(sampleUser.lastName())
                    .dateOfBirth(LocalDate.of(1990, sampleUser.index(), 15))
                    .phoneNumber(sampleUser.phoneNumber())
                    .nic(String.format("90000000%dV", sampleUser.index()))
                    .specializations(List.of("Mathematics", "Science"))
                    .hireDate(LocalDate.of(2026, 1, sampleUser.index()))
                    .build());
            return;
        }

        if (sampleUser.role() == Role.ADMIN && user.getAdminProfile() == null) {
            user.setAdminProfile(AdminProfile.builder()
                    .adminCode(adminCodeGenerator.generate())
                    .firstName(sampleUser.firstName())
                    .lastName(sampleUser.lastName())
                    .dateOfBirth(LocalDate.of(1992, sampleUser.index(), 20))
                    .phoneNumber(sampleUser.phoneNumber())
                    .nic(String.format("92000000%dV", sampleUser.index()))
                    .build());
        }
    }

    private void attachAddressIfMissing(User user) {
        if (user.getAddress() != null) {
            return;
        }

        user.setAddress(Address.builder()
                .addressLine1("Sample Address Line 1")
                .addressLine2("Sample Address Line 2")
                .city("Colombo")
                .district("Colombo")
                .postalCode("00100")
                .build());
    }

    private void ensureRealmRoleExists(RealmResource realm, Role role) {
        try {
            realm.roles().get(role.name()).toRepresentation();
        } catch (NotFoundException exception) {
            RoleRepresentation roleRepresentation = new RoleRepresentation();
            roleRepresentation.setName(role.name());
            roleRepresentation.setDescription("Sample user bootstrap role");
            realm.roles().create(roleRepresentation);
            log.info("Created Keycloak realm role {}", role.name());
        }
    }

    private void assignRealmRoleIfMissing(
            RealmResource realm,
            UsersResource users,
            String keycloakUserId,
            Role role
    ) {
        RoleRepresentation roleRepresentation = realm.roles()
                .get(role.name())
                .toRepresentation();

        RoleScopeResource realmRoles = users.get(keycloakUserId)
                .roles()
                .realmLevel();

        boolean alreadyAssigned = realmRoles.listAll().stream()
                .anyMatch(assignedRole -> role.name().equals(assignedRole.getName()));

        if (!alreadyAssigned) {
            realmRoles.add(List.of(roleRepresentation));
        }
    }

    private Optional<UserRepresentation> findUserByEmail(UsersResource users, String email) {
        String normalizedEmail = normalizeEmail(email);

        return users.searchByEmail(normalizedEmail, true).stream()
                .filter(user -> normalizeEmail(user.getEmail()).equals(normalizedEmail)
                        || normalizeEmail(user.getUsername()).equals(normalizedEmail))
                .findFirst();
    }

    private void setPermanentPassword(UsersResource users, String keycloakUserId) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(PASSWORD_CREDENTIAL_TYPE);
        credential.setValue(sampleUsersProperties.getPassword());
        credential.setTemporary(false);

        users.get(keycloakUserId).resetPassword(credential);
    }

    private SampleUser sampleUser(Role role, int index) {
        String rolePrefix = role.name().toLowerCase(Locale.ROOT);
        String displayRole = toDisplayRole(role);
        return new SampleUser(
                role,
                index,
                rolePrefix + index + "@" + normalizeDomain(sampleUsersProperties.getEmailDomain()),
                "Sample",
                displayRole + index,
                samplePhoneNumber(index)
        );
    }

    private String samplePhoneNumber(int index) {
        return String.format("077900%04d", index);
    }

    private String toDisplayRole(Role role) {
        String lowercase = role.name().toLowerCase(Locale.ROOT);
        return lowercase.substring(0, 1).toUpperCase(Locale.ROOT) + lowercase.substring(1);
    }

    private String extractCreatedUserId(URI location, String email) {
        if (location == null || location.getPath() == null || location.getPath().isBlank()) {
            throw new IllegalStateException("Keycloak did not return created sample user location for " + email);
        }

        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private void validate() {
        if (sampleUsersProperties.getUsersPerRole() < 1) {
            throw new IllegalStateException("Sample users per role must be at least 1");
        }

        if (!StringUtils.hasText(sampleUsersProperties.getEmailDomain())) {
            throw new IllegalStateException("Sample user email domain must be configured");
        }

        if (!StringUtils.hasText(sampleUsersProperties.getPassword())) {
            throw new IllegalStateException("Sample user password must be configured");
        }
    }

    private String normalizeDomain(String domain) {
        return domain.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void sleep(long retryDelayMillis) {
        try {
            Thread.sleep(Math.max(0, retryDelayMillis));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying sample users bootstrap", exception);
        }
    }

    private record SampleUser(
            Role role,
            int index,
            String email,
            String firstName,
            String lastName,
            String phoneNumber
    ) {
    }
}
