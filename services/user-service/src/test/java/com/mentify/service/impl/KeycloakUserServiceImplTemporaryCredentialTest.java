package com.mentify.service.impl;

import com.mentify.config.KeycloakProperties;
import com.mentify.service.registration.KeycloakUserProvisionRequest;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KeycloakUserServiceImplTemporaryCredentialTest {

    @Test
    void sendPasswordSetupEmail_redirectsToConfiguredFrontendLoginAfterActions() {
        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realm = mock(RealmResource.class);
        UsersResource users = mock(UsersResource.class);
        UserResource user = mock(UserResource.class);
        KeycloakProperties properties = new KeycloakProperties();
        properties.setRealm("mentify");
        properties.setFrontendClientId("mentify-web-client");
        properties.setFirstLoginRedirectUri("http://localhost:5173/login");
        when(keycloak.realm("mentify")).thenReturn(realm);
        when(realm.users()).thenReturn(users);
        when(users.get("kc-user")).thenReturn(user);

        new KeycloakUserServiceImpl(keycloak, properties).sendPasswordSetupEmail("kc-user");

        verify(user).executeActionsEmail(
                "mentify-web-client", "http://localhost:5173/login", List.of("UPDATE_PASSWORD", "VERIFY_EMAIL"));
    }

    @Test
    void createUser_setsRequiredSetupActions() {
        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realm = mock(RealmResource.class);
        UsersResource users = mock(UsersResource.class);
        KeycloakProperties properties = new KeycloakProperties();
        properties.setRealm("mentify");
        when(keycloak.realm("mentify")).thenReturn(realm);
        when(realm.users()).thenReturn(users);
        when(users.create(any(UserRepresentation.class))).thenAnswer(invocation -> {
            UserRepresentation user = invocation.getArgument(0);
            assertThat(user.getRequiredActions()).contains("UPDATE_PASSWORD", "VERIFY_EMAIL");
            assertThat(user.getCredentials()).isNull();
            return Response.created(java.net.URI.create("http://keycloak/admin/realms/mentify/users/kc-user")).build();
        });
        KeycloakUserServiceImpl service = new KeycloakUserServiceImpl(keycloak, properties);

        String keycloakId = service.createUser(new KeycloakUserProvisionRequest(
                "student@example.com", "Student", "User"));

        assertThat(keycloakId).isEqualTo("kc-user");
        verify(users).create(any(UserRepresentation.class));
    }

    @Test
    void setTemporaryPassword_usesKeycloakPasswordEndpointAndMarksCredentialTemporary() {
        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realm = mock(RealmResource.class);
        UsersResource users = mock(UsersResource.class);
        UserResource user = mock(UserResource.class);
        KeycloakProperties properties = new KeycloakProperties();
        properties.setRealm("mentify");
        when(keycloak.realm("mentify")).thenReturn(realm);
        when(realm.users()).thenReturn(users);
        when(users.get("kc-user")).thenReturn(user);
        doAnswer(invocation -> {
            CredentialRepresentation credential = invocation.getArgument(0);
            assertThat(credential.getType()).isEqualTo(CredentialRepresentation.PASSWORD);
            assertThat(credential.getValue()).isEqualTo("Secure!Temp123");
            assertThat(credential.isTemporary()).isTrue();
            return null;
        }).when(user).resetPassword(any(CredentialRepresentation.class));

        new KeycloakUserServiceImpl(keycloak, properties).setTemporaryPassword("kc-user", "Secure!Temp123");

        verify(user).resetPassword(any(CredentialRepresentation.class));
    }

    @Test
    void setTemporaryPassword_whenKeycloakPolicyRejectsPassword_returnsSafeFailure() {
        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realm = mock(RealmResource.class);
        UsersResource users = mock(UsersResource.class);
        UserResource user = mock(UserResource.class);
        KeycloakProperties properties = new KeycloakProperties();
        properties.setRealm("mentify");
        when(keycloak.realm("mentify")).thenReturn(realm);
        when(realm.users()).thenReturn(users);
        when(users.get("kc-user")).thenReturn(user);
        org.mockito.Mockito.doThrow(new WebApplicationException(Response.status(400).build()))
                .when(user).resetPassword(any(CredentialRepresentation.class));

        assertThatThrownBy(() -> new KeycloakUserServiceImpl(keycloak, properties)
                .setTemporaryPassword("kc-user", "PolicyRejected!123"))
                .isInstanceOf(com.mentify.exception.KeycloakUserCreationException.class)
                .hasMessage("Generated temporary password violates the configured Keycloak password policy")
                .hasMessageNotContaining("PolicyRejected!123");
    }
}
