# First-login authentication test plan

This plan covers the user-service contract and the manual Keycloak/email flow for temporary
credentials. Dashboard routing is a frontend responsibility; no frontend source is present in this
repository, so its route-guard and end-to-end checks must be run in the frontend repository.

## Automated user-service coverage

Run the focused tests from the repository root:

```bash
bash .github/scripts/maven-with-retry.sh -pl services/user-service -am test \
  -Dtest=TemporaryPasswordGeneratorTest,TemporaryCredentialsEmailDeliveryServiceTest,PasswordSetupEmailDispatcherTest,KeycloakUserServiceImplTemporaryCredentialTest,UserRegistrationServiceTest,DefaultKeycloakAuthenticationClientTest,AuthenticationServiceTest,AuthenticationControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Run the existing resource-server and gateway access-control tests separately:

```bash
bash .github/scripts/maven-with-retry.sh -pl cloud/api-gateway,common-lib -am test \
  -Dtest=GatewaySecurityConfigTest,SecurityConfigTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

The tests cover:

- Secure temporary-password shape and independent generation.
- Registration's Keycloak credential setup and `UPDATE_PASSWORD`/`VERIFY_EMAIL` actions.
- Keycloak rejection when a generated password violates realm policy.
- Welcome email plain text and HTML content, bounded SMTP retry, and sanitized terminal failure.
- Post-commit delivery failure propagation without logging or returning the temporary password.
- Direct-grant rejection while Keycloak reports an unfinished required action, including HTTP 428
  with no token response and no bad-password counter increment.
- Invalid temporary credentials using the same generic unauthorized response as other bad passwords.
- Successful permanent-password login after the required-action rejection, local profile checks,
  role consistency, and role metadata in the login response.
- Student, teacher, admin, and super-admin role values used by the frontend after authentication.

## Manual Keycloak and Mailpit integration check

Start the local infrastructure and user-service with SMTP configured to Mailpit. Use a fresh email
address for each run.

1. Register a student or teacher through the supported registration API (or an admin through the
   admin registration API). Confirm that the API response contains no password.
2. Confirm Mailpit receives the Mentify welcome email and Keycloak setup email. The setup email
   action contains both `UPDATE_PASSWORD` and `VERIFY_EMAIL` and returns to the configured
   `MENTIFY_LOGIN_URL` after both actions complete.
3. Before completing the action link, try the temporary password at `POST /api/v1/auth/login`.
   Expect HTTP 428, setup instructions, and no access or refresh token. Try a wrong temporary
   password and expect the generic HTTP 401 response.
4. Follow the Keycloak action link. Submit a password that violates the realm password policy and
   confirm Keycloak rejects it. Submit a compliant new password and confirmation; confirm the
   action completes and redirects to the frontend login URL.
5. Log in with the new permanent password. Confirm a token and the expected role are returned.
   Confirm the old temporary password now receives HTTP 401.
6. Call a protected API without a token and with an invalid/expired token; both must be denied.
   Call it with the valid access token and verify authorization follows the user's Keycloak role.
7. Repeat for `STUDENT`, `TEACHER`, and `ADMIN`/`SUPER_ADMIN`. In the frontend repository, assert
   that the login response's `data.user.role` maps to the configured dashboard route and that
   direct navigation to a protected dashboard without a valid session redirects to login.

If a welcome email delivery attempt fails, registration should return the safe email-delivery error
after the account transaction commits. Use the admin resend-invitation endpoint to rotate the
temporary credential and send a new setup link; verify the old temporary password is not resent.

## Environment limitations

The automated tests in this repository are unit and MVC tests using mocked Keycloak and SMTP
clients. They verify the service contract but do not prove the live Keycloak browser action flow,
SMTP delivery, or a full end-to-end registration. The frontend routing checks require the separate
frontend repository. Record the Keycloak version, realm password policy, SMTP provider, tested roles,
and pass/fail result when running the manual integration check. Do not report the manual checks as
passed until they have been run against configured services.
