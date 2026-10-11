# Sample Login Users

The repository's `.env.example` enables sample users for local development. Copy it to `.env` (if you have not already), then make sure these settings are present. Enable this only in local development or test environments:

```txt
MENTIFY_SAMPLE_USERS_ENABLED=true
MENTIFY_SAMPLE_USERS_PASSWORD=Sample@12345
```

Restart `user-service` after changing `.env`. At startup it creates or updates ready-to-login users in the Keycloak realm configured by `KEYCLOAK_REALM` (default: `mentify`) and matching local user profiles. Check the `user-service` startup logs for `Sample login users bootstrap completed`; if bootstrap is disabled, the logs say so explicitly.

If the users still do not appear, confirm that the Keycloak Admin Console is showing the same realm (`mentify` by default) and that the `user-service` environment points to the running Keycloak instance. The bootstrap requires the configured admin client service account to have the realm-management roles `manage-realm`, `view-realm`, `manage-users`, `view-users`, and `query-users`.

Default login accounts:

```txt
student1@mentify.test / Sample@12345
student2@mentify.test / Sample@12345
student3@mentify.test / Sample@12345
student4@mentify.test / Sample@12345
student5@mentify.test / Sample@12345

teacher1@mentify.test / Sample@12345
teacher2@mentify.test / Sample@12345
teacher3@mentify.test / Sample@12345
teacher4@mentify.test / Sample@12345
teacher5@mentify.test / Sample@12345

admin1@mentify.test / Sample@12345
admin2@mentify.test / Sample@12345
admin3@mentify.test / Sample@12345
admin4@mentify.test / Sample@12345
admin5@mentify.test / Sample@12345
```

Login through the backend:

```http
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json
```

```json
{
  "identifier": "student1@mentify.test",
  "password": "Sample@12345"
}
```

You can change the count, email domain, and password with:

```txt
MENTIFY_SAMPLE_USERS_PER_ROLE=5
MENTIFY_SAMPLE_USERS_EMAIL_DOMAIN=mentify.test
MENTIFY_SAMPLE_USERS_PASSWORD=Sample@12345
MENTIFY_SAMPLE_USERS_RESET_PASSWORD=true
MENTIFY_SAMPLE_USERS_RETRY_ATTEMPTS=10
MENTIFY_SAMPLE_USERS_RETRY_DELAY_MILLIS=2000
```

The bootstrap is idempotent. Restarting `user-service` does not create duplicate local users. If `MENTIFY_SAMPLE_USERS_RESET_PASSWORD=true`, the configured password is applied again on startup.
