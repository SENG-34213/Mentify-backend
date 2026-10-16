# User Registration and First Login Authentication Audit

**Branch:** `chore/MENT-91-first-login-auth-audit`  
**Scope:** Read-only review of registration, Keycloak provisioning, login, email, and first-login password setup. No runtime behavior was changed for this audit.

## Summary

The admin-managed registration flow for students and teachers, and the super-admin-managed registration flow for admins, already provisions Keycloak users with the required actions `UPDATE_PASSWORD` and `VERIFY_EMAIL`. Mentify does not assign an initial or temporary password in this flow. Keycloak sends a branded action email; the user chooses a password and verifies email through the Keycloak action page before using the Mentify login bridge.

This already provides mandatory password setup for invited accounts without generating or emailing a temporary credential. The current login bridge is a direct password grant, so it can only handle a successful token response or a generic authentication failure. It does not render or complete a Keycloak required-action challenge. Keep first-password setup on the existing email action link unless product requirements specifically require users to enter a temporary password in the Mentify login form.

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

### Legacy student registration endpoint

`POST /api/students/register` delegates to `StudentServiceImpl`, which maps the request to local user/profile/address records and saves them. It does not call `KeycloakUserService`, assign a Keycloak realm role, or send a password setup email. This path is separate from the Keycloak-backed registration workflow and should not be used for accounts expected to log in through Keycloak without first being reconciled.

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
- Do not send generated temporary passwords over email or store them in Mentify.
- Preserve the local `INVITED` status and role checks; successful Keycloak authentication must not activate incomplete or disabled local profiles.
- Never return application access tokens until the user has completed the required Keycloak setup actions.
- Preserve current login, refresh, logout, JWT validation, and role-based authorization for established accounts.

### Recommended implementation

1. Retain the existing `UPDATE_PASSWORD` and `VERIFY_EMAIL` action email as the mandatory first-password flow. It avoids temporary passwords, a new database table, and new services.
2. Confirm the deployed realm has the required actions enabled, suitable password policy configured, SMTP verified, action-link lifespan chosen, and Mentify email theme selected.
3. Confirm the setup action completion returns the user to the intended frontend login route. The Admin Client supports a redirect URI when paired with a client ID; choose and allowlist the actual frontend route before configuring it.
4. Verify the user cannot get an application token before the required action completes, using the same Keycloak/client settings used in production. The current password-grant login bridge is not an interactive action handler.
5. If the product explicitly requires entering a temporary password in the Mentify login UI, treat that as a separate design decision: prefer a Keycloak browser/standard authorization-code flow with PKCE and an enforced password-update action. Avoid adding a temporary-password store or custom identity flow unless an interactive React-owned flow is a confirmed requirement.

No extra microservice or Mentify database table is required for the recommended flow. No code change is needed merely to keep requiring password selection; the existing creation and email path already requests it.

## Compatibility and Follow-up

- Existing accounts without pending required actions keep the current login bridge behavior.
- Existing role, profile completeness, lockout, and status checks remain authoritative after Keycloak authentication.
- The public-looking legacy student endpoint does not provision a Keycloak identity. Decide whether to retire it, secure it, or connect it to the supported flow in a separate implementation task.
- Keep the database and Keycloak operation compensations covered by current registration tests. Add focused integration coverage for action completion, action-link expiry/resend, failure before local commit, email failure after commit, and attempted login before action completion when implementing any behavior change.

## Evidence Reviewed

- Student, teacher, admin, and user registration controllers/services.
- `UserRegistrationFactory`, `RegistrationRequestValidator`, and `KeycloakUserProvisionRequest`.
- Keycloak Admin Client implementation and configuration.
- Login controller, authentication service, token client, gateway/shared security setup, and Keycloak realm/email theme files.
- Registration and authentication tests, plus Keycloak setup documentation.
