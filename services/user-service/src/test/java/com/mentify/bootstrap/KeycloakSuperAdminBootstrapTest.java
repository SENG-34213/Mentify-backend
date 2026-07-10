package com.mentify.bootstrap;

import com.mentify.config.KeycloakProperties;
import com.mentify.enums.Role;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakSuperAdminBootstrapTest {

    @Mock
    private Keycloak keycloak;

    @Mock
    private RealmResource realm;

    @Mock
    private UsersResource users;

    @Mock
    private UserResource userResource;

    @Mock
    private RoleMappingResource roleMappingResource;

    @Mock
    private RoleScopeResource roleScopeResource;

    @Mock
    private RolesResource roles;

    @Mock
    private RoleResource roleResource;

    @Mock
    private Response createUserResponse;

    private KeycloakProperties keycloakProperties;
    private KeycloakSuperAdminBootstrap bootstrap;

    @BeforeEach
    void setUp() {
        keycloakProperties = new KeycloakProperties();
        keycloakProperties.setRealm("mentify");
        keycloakProperties.getBootstrapSuperAdmin().setEnabled(true);
        keycloakProperties.getBootstrapSuperAdmin().setEmail("superadmin@gmail.com");
        keycloakProperties.getBootstrapSuperAdmin().setPassword("SuperAdmin@123");

        bootstrap = new KeycloakSuperAdminBootstrap(keycloak, keycloakProperties);
    }

    @Test
    void bootstrapOnce_whenSuperAdminDoesNotExist_createsUserSetsPasswordAndAssignsRole() {
        RoleRepresentation roleRepresentation = superAdminRole();
        when(keycloak.realm("mentify")).thenReturn(realm);
        when(realm.users()).thenReturn(users);
        when(realm.roles()).thenReturn(roles);
        when(roles.get(Role.SUPER_ADMIN.name())).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(roleRepresentation);
        when(users.searchByEmail("superadmin@gmail.com", true)).thenReturn(List.of());
        when(users.create(any(UserRepresentation.class))).thenReturn(createUserResponse);
        when(createUserResponse.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(createUserResponse.getLocation()).thenReturn(URI.create("http://localhost/admin/realms/mentify/users/kc-user-id"));
        when(users.get("kc-user-id")).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
        when(roleScopeResource.listAll()).thenReturn(List.of());

        bootstrap.bootstrapOnce();

        ArgumentCaptor<UserRepresentation> userCaptor = ArgumentCaptor.forClass(UserRepresentation.class);
        verify(users).create(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("superadmin@gmail.com");
        assertThat(userCaptor.getValue().isEmailVerified()).isTrue();
        assertThat(userCaptor.getValue().isEnabled()).isTrue();

        ArgumentCaptor<CredentialRepresentation> credentialCaptor =
                ArgumentCaptor.forClass(CredentialRepresentation.class);
        verify(userResource).resetPassword(credentialCaptor.capture());
        assertThat(credentialCaptor.getValue().getValue()).isEqualTo("SuperAdmin@123");
        assertThat(credentialCaptor.getValue().isTemporary()).isFalse();

        verify(roleScopeResource).add(List.of(roleRepresentation));
    }

    @Test
    void bootstrapOnce_whenSuperAdminExistsAndRoleExists_doesNotCreateUserOrDuplicateRole() {
        UserRepresentation existingUser = new UserRepresentation();
        existingUser.setId("existing-kc-user-id");
        existingUser.setUsername("superadmin@gmail.com");
        existingUser.setEmail("superadmin@gmail.com");

        when(keycloak.realm("mentify")).thenReturn(realm);
        when(realm.users()).thenReturn(users);
        when(realm.roles()).thenReturn(roles);
        when(roles.get(Role.SUPER_ADMIN.name())).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(superAdminRole());
        when(users.searchByEmail("superadmin@gmail.com", true)).thenReturn(List.of(existingUser));
        when(users.get("existing-kc-user-id")).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
        when(roleScopeResource.listAll()).thenReturn(List.of(superAdminRole()));

        bootstrap.bootstrapOnce();

        verify(users, never()).create(any(UserRepresentation.class));
        verify(roleScopeResource, never()).add(any());
        verify(userResource).resetPassword(any(CredentialRepresentation.class));
    }

    @Test
    void bootstrapOnce_whenSuperAdminRoleIsMissing_createsRealmRole() {
        RoleRepresentation roleRepresentation = superAdminRole();
        when(keycloak.realm("mentify")).thenReturn(realm);
        when(realm.users()).thenReturn(users);
        when(realm.roles()).thenReturn(roles);
        when(roles.get(Role.SUPER_ADMIN.name())).thenReturn(roleResource);
        when(roleResource.toRepresentation())
                .thenThrow(new NotFoundException("missing"))
                .thenReturn(roleRepresentation);
        when(users.searchByEmail("superadmin@gmail.com", true)).thenReturn(List.of(existingSuperAdmin()));
        when(users.get("existing-kc-user-id")).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
        when(roleScopeResource.listAll()).thenReturn(List.of());

        bootstrap.bootstrapOnce();

        ArgumentCaptor<RoleRepresentation> roleCaptor = ArgumentCaptor.forClass(RoleRepresentation.class);
        verify(roles).create(roleCaptor.capture());
        assertThat(roleCaptor.getValue().getName()).isEqualTo(Role.SUPER_ADMIN.name());
        verify(roleScopeResource).add(List.of(roleRepresentation));
    }

    @Test
    void bootstrapOnce_whenDisabled_doesNothing() {
        keycloakProperties.getBootstrapSuperAdmin().setEnabled(false);

        bootstrap.bootstrapOnce();

        verify(keycloak, never()).realm(any());
    }

    private RoleRepresentation superAdminRole() {
        RoleRepresentation roleRepresentation = new RoleRepresentation();
        roleRepresentation.setName(Role.SUPER_ADMIN.name());
        return roleRepresentation;
    }

    private UserRepresentation existingSuperAdmin() {
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setId("existing-kc-user-id");
        userRepresentation.setUsername("superadmin@gmail.com");
        userRepresentation.setEmail("superadmin@gmail.com");
        return userRepresentation;
    }
}
