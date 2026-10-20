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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KeycloakUserServiceImplTemporaryCredentialTest {

    @Test
    void createUser_doesNotRequireActionEmailBeforeTemporaryPasswordLogin() {
        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realm = mock(RealmResource.class);
        UsersResource users = mock(UsersResource.class);
        KeycloakProperties properties = new KeycloakProperties();
        properties.setRealm("mentify");
        when(keycloak.realm("mentify")).thenReturn(realm);
        when(realm.users()).thenReturn(users);
        when(users.create(any(UserRepresentation.class))).thenAnswer(invocation -> {
            UserRepresentation user = invocation.getArgument(0);
            assertThat(user.getRequiredActions()).isEmpty();
            assertThat(user.isEmailVerified()).isTrue();
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
    void setInitialPassword_usesKeycloakPasswordEndpointAndAllowsPasswordGrantLogin() {
        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realm = mock(RealmResource.class);
        UsersResource users = mock(UsersResource.class);
        UserResource user = mock(UserResource.class);
        KeycloakProperties properties = new KeycloakProperties();
        properties.setRealm("mentify");
        when(keycloak.realm("mentify")).thenReturn(realm);
        when(realm.users()).thenReturn(users);
        when(users.get("kc-user")).thenReturn(user);
        when(user.toRepresentation()).thenReturn(new UserRepresentation());
        doAnswer(invocation -> {
            CredentialRepresentation credential = invocation.getArgument(0);
            assertThat(credential.getType()).isEqualTo(CredentialRepresentation.PASSWORD);
            assertThat(credential.getValue()).isEqualTo("Secure!Temp123");
            assertThat(credential.isTemporary()).isFalse();
            return null;
        }).when(user).resetPassword(any(CredentialRepresentation.class));

        new KeycloakUserServiceImpl(keycloak, properties).setInitialPassword("kc-user", "Secure!Temp123");

        org.mockito.ArgumentCaptor<UserRepresentation> userCaptor =
                org.mockito.ArgumentCaptor.forClass(UserRepresentation.class);
        verify(user).update(userCaptor.capture());
        assertThat(userCaptor.getValue().getRequiredActions()).isEmpty();
        assertThat(userCaptor.getValue().isEmailVerified()).isTrue();
        verify(user).resetPassword(any(CredentialRepresentation.class));
    }

    @Test
    void setInitialPassword_whenKeycloakPolicyRejectsPassword_returnsSafeFailure() {
        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realm = mock(RealmResource.class);
        UsersResource users = mock(UsersResource.class);
        UserResource user = mock(UserResource.class);
        KeycloakProperties properties = new KeycloakProperties();
        properties.setRealm("mentify");
        when(keycloak.realm("mentify")).thenReturn(realm);
        when(realm.users()).thenReturn(users);
        when(users.get("kc-user")).thenReturn(user);
        when(user.toRepresentation()).thenReturn(new UserRepresentation());
        org.mockito.Mockito.doThrow(new WebApplicationException(Response.status(400).build()))
                .when(user).resetPassword(any(CredentialRepresentation.class));

        assertThatThrownBy(() -> new KeycloakUserServiceImpl(keycloak, properties)
                .setInitialPassword("kc-user", "PolicyRejected!123"))
                .isInstanceOf(com.mentify.exception.KeycloakUserCreationException.class)
                .hasMessage("Generated temporary password violates the configured Keycloak password policy")
                .hasMessageNotContaining("PolicyRejected!123");
    }
}
