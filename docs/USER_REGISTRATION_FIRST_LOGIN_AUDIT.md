# User Registration and First Login Authentication Audit

**Branch:** `chore/MENT-91-first-login-auth-audit`  
**Scope:** Read-only review of registration, Keycloak provisioning, login, email, and first-login password setup. No runtime behavior was changed for this audit.

## Summary

The admin-managed registration flow for students and teachers, the legacy student endpoint, and the super-admin-managed registration flow for admins provision Keycloak users with the required actions `UPDATE_PASSWORD` and `VERIFY_EMAIL`. MENT-92 adds a temporary credential assigned through Keycloak as temporary and delivered by user-service SMTP. Keycloak also sends a branded action email; the user chooses a permanent password and verifies email through the Keycloak action page before using the Mentify login bridge.

The current login bridge is a direct password grant, so it can only handle a successful token response or a generic authentication failure. Keycloak does not support required actions through that grant; users must complete the separate Keycloak setup email before the bridge can issue a token. The bridge does not render the required-action challenge itself. Verify the frontend's first-login path against the deployed Keycloak browser flow. [Keycloak documents these Direct Grant limitations](https://github.com/keycloak/keycloak/blob/main/docs/guides/securing-apps/partials/oidc/supported-grant-types.adoc).

One older registration path differs: `POST /api/students/register` uses `StudentServiceImpl` and saves a local student without provisioning a Keycloak user or sending an invitation. `TeacherController` currently exposes listing only. The intended supported teacher creation path is the admin-managed users endpoint. These route differences should be clarified before frontend work relies on them.

## Existing Flows

### Admin-managed student and teacher registration

1. `POST /api/v1/admin/users/register` (also exposed through `UserRegistration`) requires `ADMIN` or `SUPER_ADMIN`.
2. `RegistrationRequestValidator` permits only `STUDENT` and `TEACHER`, and validates the matching profile and address details.
3. `UserRegistrationService` rejects duplicate local email, creates a Keycloak user, assigns the realm role, creates the local user/profile/address as `INVITED`, and saves it.
4. `UserRegistrationFactory` creates the role-specific local profile and associates the Keycloak subject ID.
5. `PasswordSetupEmailDispatcher` sends the Keycloak action email only after the local transaction commits. Delivery errors are logged; registration stays committed and an administrator can use the resend-invitation endpoint.
6. Failures before commit trigger best-effort deletion of the newly created Keycloak user.

### Admin registration

1. `POST /api/v1/admin/users/admins/register` (also exposed through `UserRegistration`) requires `SUPER_ADMIN`.
2. `AdminRegistrationService` creates the Keycloak user, assigns the `ADMIN` realm role, creates the local admin profile in `INVITED` state, and schedules the same post-commit setup email.
3. It uses the same Keycloak provisioning request and compensation approach as student/teacher creation.

### Student registration endpoint

`POST /api/students/register` delegates to `StudentServiceImpl` and uses the legacy student mapper/profile defaults. MENT-92 aligns its identity flow: it provisions a Keycloak `STUDENT`, assigns a temporary password and role, saves the local profile as `INVITED`, and queues the same post-commit credential and setup emails. The legacy request's `password` field remains accepted for payload compatibility but is optional, excluded from request `toString()`, and ignored; Mentify does not persist or return it.

### Keycloak provisioning and setup email

- `KeycloakUserServiceImpl.createUser` creates an enabled, email-unverified Keycloak user and sets required actions `UPDATE_PASSWORD` and `VERIFY_EMAIL`.
- No password credential is assigned during this operation.
- `sendPasswordSetupEmail` uses Keycloak Admin Client `executeActionsEmail` with the same two actions. Resending an invitation is restricted to local accounts in `INVITED` state with a linked Keycloak ID.
- The Admin Client uses the confidential backend client credentials and service-account authentication. Those credentials remain server-side.
- The bundled Keycloak email theme supplies branded HTML and text for the action link. SMTP is configured in Keycloak; the notification service and Spring Mail are not used by this registration path.
- The imported realm has self-registration and the reset-password login-page option disabled. Realm mail settings and password policies must still be verified in the deployed Keycloak realm; the JSON export does not contain SMTP or an explicit password policy in the inspected sections.

Keycloak documents `execute-actions-email` as a way to send users a link that asks them to perform actions such as `VERIFY_EMAIL` and `UPDATE_PASSWORD` ([Admin Client API](https://www.keycloak.org/docs-api/26.7.3/javadocs/org/keycloak/admin/client/resource/UserResource.html)). This keeps password choice and validation within the identity provider.

### Login bridge, JWT handling, and account activation

1. `POST /api/v1/auth/login` accepts the identifier and password.
2. `DefaultKeycloakAuthenticationClient` sends them to the configured Keycloak token endpoint using the password grant and backend client credentials.
3. Invalid credentials map to a generic authentication failure; other Keycloak errors map to service unavailable.
4. The client parses the returned access-token claims for subject, email, and realm roles. The gateway and resource services validate JWT signatures against Keycloak; the bridge does not mint Mentify tokens.
5. `AuthenticationService` loads the local profile by Keycloak subject (with email fallback), checks local role against Keycloak realm roles, activates a complete and unlocked local `INVITED` profile, enforces account status, records login data, and returns token and approved profile fields.
6. Incomplete, disabled, suspended, locked, or role-mismatched local accounts do not receive a successful login response.

The direct password grant does not provide an interactive required-action screen in this backend response contract. The action email is currently the first-password setup experience. Do not rely on the login bridge to complete `UPDATE_PASSWORD` for an account that has not finished the action link.

### Email and frontend

Registration/setup email delivery is performed by Keycloak SMTP using the `mentify` email theme. The user-service dispatches the action via the Admin Client and handles delivery failure after commit with an admin resend path.

No frontend source tree or package manifest was found in this backend repository, so login routing, first-login UX, token storage, and React handling of required-action outcomes could not be audited. The documented local frontend client uses standard flow, while the backend bridge uses direct grant; deployment-specific client settings should be checked against the actual frontend.

## Reusable Components

- `UserRegistrationService`, `AdminRegistrationService`: role-specific local and Keycloak provisioning orchestration with compensation.
- `KeycloakUserService` / `KeycloakUserServiceImpl`: Keycloak user creation, realm-role assignment, required-action email, deletion, and resend support.
- `PasswordSetupEmailDispatcher`: post-commit send behavior with safe registration durability on SMTP failure.
- `KeycloakAdminClientConfig` and `KeycloakProperties`: backend-only Admin Client credentials and realm configuration.
- `DefaultKeycloakAuthenticationClient`: existing token exchange, refresh, and logout integration.
- `AuthenticationService`: local account, role, and profile checks around Keycloak authentication.
- `KeycloakJwtAuthenticationConverter`, gateway and shared `SecurityConfig`: JWT validation and role mapping for protected endpoints.
- Existing Keycloak email theme: branded HTML/text setup message.

## First-login Requirements and Recommended Approach

### Requirements

- A newly invited user must choose a password that meets the active Keycloak realm policy before using Mentify.
- The setup action must expire, be one-time, and be safely resendable by an authorized administrator.
- Do not store temporary passwords in Mentify, expose them in API responses, or log them. MENT-92 sends the generated credential by email because this is the requested delivery channel.
- Preserve the local `INVITED` status and role checks; successful Keycloak authentication must not activate incomplete or disabled local profiles.
- Never return application access tokens until the user has completed the required Keycloak setup actions.
- Preserve current login, refresh, logout, JWT validation, and role-based authorization for established accounts.

### Recommended implementation

1. Retain the existing `UPDATE_PASSWORD` and `VERIFY_EMAIL` action email as the mandatory first-password flow. MENT-92 assigns the temporary credential through the Keycloak Admin Client and delivers it separately by user-service SMTP; Mentify does not persist it.
2. Confirm the deployed realm has the required actions enabled, suitable password policy configured, SMTP verified, action-link lifespan chosen, and Mentify email theme selected.
3. Confirm the setup action completion returns the user to the intended frontend login route. The Admin Client supports a redirect URI when paired with a client ID; choose and allowlist the actual frontend route before configuring it.
4. Verify the user cannot get an application token before the required action completes, using the same Keycloak/client settings used in production. The current password-grant login bridge is not an interactive action handler.
5. The backend login bridge uses direct password grant and has no required-action response flow. If the frontend must collect the temporary password and perform the forced update without leaving Mentify, implement a supported browser/standard authorization-code flow with PKCE or a carefully scoped API flow in a separate task.

No extra microservice or Mentify database table was introduced. MENT-92 reuses the existing Keycloak required-action email and the shared SMTP configuration to assign and deliver the one-time temporary credential.

## Compatibility and Follow-up

- Existing accounts without pending required actions keep the current login bridge behavior.
- Existing role, profile completeness, lockout, and status checks remain authoritative after Keycloak authentication.
- The legacy student endpoint now provisions a Keycloak identity, but it retains its older profile mapping and request model. Consolidating it with the admin-managed registration factory remains a separate cleanup.
- Keep the database and Keycloak operation compensations covered by current registration tests. Add focused integration coverage for action completion, action-link expiry/resend, failure before local commit, email failure after commit, and attempted login before action completion when implementing any behavior change.

## Evidence Reviewed

- Student, teacher, admin, and user registration controllers/services.
- `UserRegistrationFactory`, `RegistrationRequestValidator`, and `KeycloakUserProvisionRequest`.
- Keycloak Admin Client implementation and configuration.
- Login controller, authentication service, token client, gateway/shared security setup, and Keycloak realm/email theme files.
- Registration and authentication tests, plus Keycloak setup documentation.
