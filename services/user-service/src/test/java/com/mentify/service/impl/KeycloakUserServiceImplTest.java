package com.mentify.service.impl;

import com.mentify.config.KeycloakProperties;
import com.mentify.service.registration.KeycloakUserProvisionRequest;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KeycloakUserServiceImplTest {

    private Keycloak keycloak;
    private RealmResource realmResource;
    private UsersResource usersResource;
    private KeycloakUserServiceImpl userService;

    @BeforeEach
    void setUp() {
        keycloak = mock(Keycloak.class);
        realmResource = mock(RealmResource.class);
        usersResource = mock(UsersResource.class);
        KeycloakProperties properties = new KeycloakProperties();
        properties.setRealm("mentify");

        when(keycloak.realm("mentify")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        userService = new KeycloakUserServiceImpl(keycloak, properties);
    }

    @Test
    void createUser_setsGeneratedPasswordAsKeycloakCredential() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(response.getLocation()).thenReturn(URI.create("/admin/realms/mentify/users/user-id"));
        when(usersResource.create(org.mockito.ArgumentMatchers.any(UserRepresentation.class))).thenReturn(response);
        KeycloakUserProvisionRequest request =
                new KeycloakUserProvisionRequest("student@example.com", "Student", "User");

        String userId = userService.createUser(request);

        assertThat(userId).isEqualTo("user-id");
        var userCaptor = forClass(UserRepresentation.class);
        verify(usersResource).create(userCaptor.capture());
        UserRepresentation createdUser = userCaptor.getValue();
        assertThat(createdUser.getRequiredActions()).isNull();
        assertThat(createdUser.getCredentials()).isNull();
    }

    @Test
    void setInitialPassword_setsNonTemporaryKeycloakCredential() {
        UserResource userResource = mock(UserResource.class);
        when(usersResource.get("user-id")).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(new UserRepresentation());

        userService.setInitialPassword("user-id", "A7!bcD2x");

        var credentialCaptor = forClass(CredentialRepresentation.class);
        verify(userResource).resetPassword(credentialCaptor.capture());
        assertPassword(List.of(credentialCaptor.getValue()), "A7!bcD2x");
    }

    @Test
    void resetPassword_setsNewKeycloakCredential() {
        UserResource userResource = mock(UserResource.class);
        when(usersResource.get("user-id")).thenReturn(userResource);

        userService.resetPassword("user-id", "B4@deF6g");

        var credentialCaptor = forClass(CredentialRepresentation.class);
        verify(userResource).resetPassword(credentialCaptor.capture());
        assertPassword(List.of(credentialCaptor.getValue()), "B4@deF6g");
    }

    private void assertPassword(List<CredentialRepresentation> credentials, String expectedPassword) {
        assertThat(credentials).hasSize(1);
        CredentialRepresentation credential = credentials.get(0);
        assertThat(credential.getType()).isEqualTo(CredentialRepresentation.PASSWORD);
        assertThat(credential.getValue()).isEqualTo(expectedPassword);
        assertThat(credential.isTemporary()).isFalse();
    }
}
