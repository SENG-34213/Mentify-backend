package com.mentify.bootstrap;

import com.mentify.config.KeycloakProperties;
import com.mentify.enums.Role;
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
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakSuperAdminBootstrap implements ApplicationRunner {

    private static final String PASSWORD_CREDENTIAL_TYPE = "password";

    private final Keycloak keycloak;
    private final KeycloakProperties keycloakProperties;

    @Override
    public void run(ApplicationArguments args) {
        KeycloakProperties.BootstrapSuperAdmin properties = keycloakProperties.getBootstrapSuperAdmin();
        if (!properties.isEnabled()) {
            return;
        }

        validate(properties);
        bootstrapWithRetry(properties);
    }

    void bootstrapOnce() {
        KeycloakProperties.BootstrapSuperAdmin properties = keycloakProperties.getBootstrapSuperAdmin();
        if (!properties.isEnabled()) {
            return;
        }

        validate(properties);
        RealmResource realm = keycloak.realm(keycloakProperties.getRealm());
        UsersResource users = realm.users();

        ensureSuperAdminRoleExists(realm);

        Optional<UserRepresentation> existingUser = findUserByEmail(users, properties.getEmail());
        String keycloakUserId;
        if (existingUser.isPresent()) {
            keycloakUserId = existingUser.get().getId();
            if (properties.isResetPasswordOnStartup()) {
                setPermanentPassword(users, keycloakUserId, properties.getPassword());
            }
        } else {
            keycloakUserId = createSuperAdminUser(users, properties);
        }

        assignSuperAdminRoleIfMissing(realm, users, keycloakUserId);

        log.info("Keycloak SUPER_ADMIN bootstrap completed for email={}", properties.getEmail());
    }

    private void bootstrapWithRetry(KeycloakProperties.BootstrapSuperAdmin properties) {
        int attempts = Math.max(1, properties.getRetryAttempts());

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                bootstrapOnce();
                return;
            } catch (NotAuthorizedException | ForbiddenException exception) {
                throw new IllegalStateException(
                        "Keycloak SUPER_ADMIN bootstrap is not authorized. Verify the "
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
                        "Keycloak SUPER_ADMIN bootstrap attempt {}/{} failed. Retrying in {} ms",
                        attempt,
                        attempts,
                        properties.getRetryDelayMillis(),
                        exception
                );
                sleep(properties.getRetryDelayMillis());
            }
        }
    }

    private void ensureSuperAdminRoleExists(RealmResource realm) {
        String roleName = Role.SUPER_ADMIN.name();
        try {
            realm.roles().get(roleName).toRepresentation();
        } catch (NotFoundException exception) {
            RoleRepresentation roleRepresentation = new RoleRepresentation();
            roleRepresentation.setName(roleName);
            roleRepresentation.setDescription("System bootstrap super administrator role");
            realm.roles().create(roleRepresentation);
            log.info("Created Keycloak realm role {}", roleName);
        }
    }

    private Optional<UserRepresentation> findUserByEmail(UsersResource users, String email) {
        String normalizedEmail = normalizeEmail(email);

        return users.searchByEmail(normalizedEmail, true).stream()
                .filter(user -> normalizeEmail(user.getEmail()).equals(normalizedEmail)
                        || normalizeEmail(user.getUsername()).equals(normalizedEmail))
                .findFirst();
    }

    private String createSuperAdminUser(
            UsersResource users,
            KeycloakProperties.BootstrapSuperAdmin properties
    ) {
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(normalizeEmail(properties.getEmail()));
        userRepresentation.setEmail(normalizeEmail(properties.getEmail()));
        userRepresentation.setFirstName(properties.getFirstName());
        userRepresentation.setLastName(properties.getLastName());
        userRepresentation.setEnabled(true);
        userRepresentation.setEmailVerified(true);

        try (Response response = users.create(userRepresentation)) {
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                throw new IllegalStateException(
                        "Failed to create Keycloak SUPER_ADMIN user. Status: " + response.getStatus()
                );
            }

            String keycloakUserId = extractCreatedUserId(response.getLocation());
            setPermanentPassword(users, keycloakUserId, properties.getPassword());
            log.info("Created Keycloak SUPER_ADMIN user email={}", properties.getEmail());
            return keycloakUserId;
        }
    }

    private void setPermanentPassword(UsersResource users, String keycloakUserId, String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(PASSWORD_CREDENTIAL_TYPE);
        credential.setValue(password);
        credential.setTemporary(false);

        users.get(keycloakUserId).resetPassword(credential);
    }

    private void assignSuperAdminRoleIfMissing(RealmResource realm, UsersResource users, String keycloakUserId) {
        RoleRepresentation roleRepresentation = realm.roles()
                .get(Role.SUPER_ADMIN.name())
                .toRepresentation();

        RoleScopeResource realmRoles = users.get(keycloakUserId)
                .roles()
                .realmLevel();

        boolean alreadyAssigned = realmRoles.listAll().stream()
                .anyMatch(role -> Role.SUPER_ADMIN.name().equals(role.getName()));

        if (!alreadyAssigned) {
            realmRoles.add(List.of(roleRepresentation));
            log.info("Assigned Keycloak realm role {} to bootstrap user", Role.SUPER_ADMIN.name());
        }
    }

    private String extractCreatedUserId(URI location) {
        if (location == null || location.getPath() == null || location.getPath().isBlank()) {
            throw new IllegalStateException("Keycloak did not return created SUPER_ADMIN user location");
        }

        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private void validate(KeycloakProperties.BootstrapSuperAdmin properties) {
        if (!StringUtils.hasText(properties.getEmail())) {
            throw new IllegalStateException("Keycloak SUPER_ADMIN bootstrap email must be configured");
        }

        if (!StringUtils.hasText(properties.getPassword())) {
            throw new IllegalStateException("Keycloak SUPER_ADMIN bootstrap password must be configured");
        }
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
            throw new IllegalStateException("Interrupted while retrying Keycloak SUPER_ADMIN bootstrap", exception);
        }
    }
}
