package com.mentify.service.impl;

import com.mentify.config.KeycloakProperties;
import com.mentify.exception.KeycloakEmailActionException;
import com.mentify.exception.KeycloakRoleAssignmentException;
import com.mentify.exception.KeycloakUserCreationException;
import com.mentify.service.KeycloakUserService;
import com.mentify.service.registration.KeycloakUserProvisionRequest;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakUserServiceImpl implements KeycloakUserService {

    private static final List<String> PASSWORD_RESET_ACTIONS = List.of("UPDATE_PASSWORD");

    private final Keycloak keycloak;
    private final KeycloakProperties keycloakProperties;

    @Override
    public String createUser(KeycloakUserProvisionRequest request) {
        return createUserInternal(request);
    }

    @Override
    public void setInitialPassword(String keycloakUserId, String initialPassword) {
        if (initialPassword == null || initialPassword.isBlank()) {
            throw new IllegalArgumentException("Initial password is required");
        }
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(initialPassword);
        credential.setTemporary(false);
        try {
            var userResource = realmUsers().get(keycloakUserId);
            UserRepresentation user = userResource.toRepresentation();
            user.setEmailVerified(true);
            user.setRequiredActions(List.of());
            userResource.update(user);
            userResource.resetPassword(credential);
        } catch (WebApplicationException exception) {
            if (exception.getResponse() != null && exception.getResponse().getStatus() == 400) {
                throw new KeycloakUserCreationException(
                        "Generated temporary password violates the configured Keycloak password policy");
            }
            throw new KeycloakUserCreationException("Failed to assign initial Keycloak password");
        } catch (ProcessingException exception) {
            throw new KeycloakUserCreationException("Failed to assign initial Keycloak password");
        }
    }

    private String createUserInternal(KeycloakUserProvisionRequest request) {
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(request.getEmail());
        userRepresentation.setEmail(request.getEmail());
        userRepresentation.setFirstName(request.getFirstName());
        userRepresentation.setLastName(request.getLastName());
        userRepresentation.setEnabled(true);
        userRepresentation.setEmailVerified(true);
        userRepresentation.setRequiredActions(List.of());
        try (Response response = realmUsers().create(userRepresentation)) {
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                throw new KeycloakUserCreationException(buildCreateUserFailureMessage(response));
            }

            return extractCreatedUserId(response.getLocation());
        } catch (KeycloakUserCreationException exception) {
            log.error("Keycloak user creation failed");

            throw exception;
        } catch (ProcessingException | WebApplicationException exception) {
            throw new KeycloakUserCreationException("Failed to create user in Keycloak", exception);
        }
    }

    @Override
    public void assignRealmRole(String keycloakUserId, String roleName) {
        try {
            RoleRepresentation roleRepresentation = keycloak.realm(keycloakProperties.getRealm())
                    .roles()
                    .get(roleName)
                    .toRepresentation();

            realmUsers()
                    .get(keycloakUserId)
                    .roles()
                    .realmLevel()
                    .add(List.of(roleRepresentation));
        } catch (NotFoundException exception) {
            throw new KeycloakRoleAssignmentException("Keycloak realm role does not exist: " + roleName, exception);
        } catch (ProcessingException | WebApplicationException exception) {
            throw new KeycloakRoleAssignmentException("Failed to assign Keycloak realm role: " + roleName, exception);
        }
    }

    @Override
    public void sendPasswordResetEmail(String keycloakUserId) {
        try {
            realmUsers()
                    .get(keycloakUserId)
                    .executeActionsEmail(PASSWORD_RESET_ACTIONS);
        } catch (ProcessingException | WebApplicationException exception) {
            throw new KeycloakEmailActionException("Failed to send password reset email through Keycloak", exception);
        }
    }

    @Override
    public void deleteUser(String keycloakUserId) {
        try (Response response = realmUsers().delete(keycloakUserId)) {
            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                log.debug("Keycloak user {} was already deleted", keycloakUserId);
                return;
            }

            if (response.getStatus() != Response.Status.NO_CONTENT.getStatusCode()) {
                throw new KeycloakUserCreationException(
                        "Failed to delete Keycloak user during compensation. Status: " + response.getStatus()
                );
            }
        } catch (NotFoundException exception) {
            log.debug("Keycloak user {} was already deleted", keycloakUserId);
        } catch (ProcessingException | WebApplicationException exception) {
            throw new KeycloakUserCreationException("Failed to delete Keycloak user during compensation", exception);
        }
    }

    private org.keycloak.admin.client.resource.UsersResource realmUsers() {
        return keycloak.realm(keycloakProperties.getRealm()).users();
    }

    private String extractCreatedUserId(URI location) {
        if (location == null || location.getPath() == null || location.getPath().isBlank()) {
            throw new KeycloakUserCreationException("Keycloak did not return created user location");
        }

        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private String buildCreateUserFailureMessage(Response response) {
        if (response.getStatus() == Response.Status.CONFLICT.getStatusCode()) {
            return "User already exists in Keycloak";
        }

        return "Failed to create user in Keycloak. Status: " + response.getStatus();
    }
}
