# Admin User Registration Flow: Postman Testing Guide

## Scope

This guide is for testing the `user-service` admin-driven registration flow through the API gateway with Keycloak.

Supported flows:

```txt
ADMIN or SUPER_ADMIN registers STUDENT
ADMIN or SUPER_ADMIN registers TEACHER
SUPER_ADMIN registers ADMIN
ADMIN or SUPER_ADMIN resends invitation email
```

Out of scope:

```txt
SUPER_ADMIN creation
Password reset API
Google login
MFA
Course enrollment
Student/teacher class assignment
```

## Required Services

Start these before testing:

```txt
Keycloak:     http://localhost:8180
api-gateway:  http://localhost:8080
user-service: registered behind api-gateway
PostgreSQL:   user-service database running
```

Keycloak realm/client assumptions:

```txt
Realm: mentify
Frontend client: mentify-web-client
Backend client: mentify-backend-client
Realm roles: STUDENT, TEACHER, ADMIN, SUPER_ADMIN
```

Keycloak SMTP must be configured because password setup emails are sent by Keycloak.

## Pre-Test Keycloak Setup

Before testing these endpoints, create seed users in Keycloak for authentication.

Required test users:

```txt
One SUPER_ADMIN user
One ADMIN user
```

These users are needed only to call the protected registration endpoints from Postman. The `SUPER_ADMIN` creation flow is intentionally not implemented in this ticket, so create the first `SUPER_ADMIN` manually in Keycloak or through a separate seed/bootstrap process.

Minimum setup for the seed `SUPER_ADMIN` user:

```txt
1. Create user in Keycloak realm mentify.
2. Set email and username.
3. Set enabled = ON.
4. Set a temporary or permanent password.
5. Assign realm role SUPER_ADMIN.
6. Login through Postman and use the access token to create ADMIN users.
```

Minimum setup for a seed `ADMIN` user, if needed for testing student/teacher registration:

```txt
1. Create user in Keycloak realm mentify.
2. Set email and username.
3. Set enabled = ON.
4. Set a temporary or permanent password.
5. Assign realm role ADMIN.
```

## Keycloak SMTP Setup

Password setup emails are sent by Keycloak, not by `user-service`. Configure SMTP before testing registration emails.

Keycloak Admin Console path:

```txt
Realm mentify -> Realm settings -> Email
```

Typical fields to configure:

```txt
From: no-reply@your-domain.com or your test email
From display name: Mentify
Reply to: no-reply@your-domain.com or your test email
Host: SMTP host, for example smtp.gmail.com
Port: SMTP port, for example 587
Encryption: SSL or STARTTLS based on provider
Authentication: ON if provider requires login
Username: SMTP username or your test email
Password: SMTP password or app password
```

For Gmail-style testing, use an app password, not your normal Gmail password.

After saving SMTP settings:

```txt
1. Click Test connection in Keycloak.
2. Confirm the test email is received.
3. Then test user registration from Postman.
```

If SMTP is not configured correctly, registration may still create the local/Keycloak user, but the password setup email may fail. In that case, fix SMTP and call the resend invitation endpoint.

## 1. Login And Get Access Token

Use this request to login as `ADMIN` or `SUPER_ADMIN`.

```http
POST http://localhost:8180/realms/mentify/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded
```

Body type: `x-www-form-urlencoded`

```txt
client_id=mentify-web-client
grant_type=password
username=superadmin@gmail.com
password=YOUR_PASSWORD
```

Copy the `access_token` from the response. For protected requests, add this header:

```http
Authorization: Bearer <access_token>
```

## 2. Register Student

Allowed roles:

```txt
ADMIN
SUPER_ADMIN
```

Request:

```http
POST http://localhost:8080/api/v1/admin/users/register
Authorization: Bearer <access_token>
Content-Type: application/json
```

Body:

```json
{
  "email": "student1@gmail.com",
  "firstName": "Kamal",
  "lastName": "Perera",
  "phoneNumber": "0771234567",
  "role": "STUDENT",
  "studentProfile": {
    "dateOfBirth": "2010-05-12",
    "guardianName": "Sunil Perera",
    "guardianPhone": "0771112222",
    "attendanceMode": "PHYSICAL",
    "grade": "7"
  },
  "address": {
    "addressLine1": "No 10",
    "addressLine2": "Main Street",
    "city": "Colombo",
    "district": "Colombo",
    "postalCode": "00100"
  }
}
```

Expected status:

```txt
201 Created
```

Expected code format:

```txt
studentProfile.studentId = TIT-07-001
```

The number increases per grade:

```txt
TIT-07-001
TIT-07-002
TIT-08-001
```

## 3. Register Teacher

Allowed roles:

```txt
ADMIN
SUPER_ADMIN
```

Request:

```http
POST http://localhost:8080/api/v1/admin/users/register
Authorization: Bearer <access_token>
Content-Type: application/json
```

Body:

```json
{
  "email": "teacher1@gmail.com",
  "firstName": "Nimal",
  "lastName": "Silva",
  "phoneNumber": "0772345678",
  "role": "TEACHER",
  "teacherProfile": {
    "dateOfBirth": "1990-02-20",
    "nic": "901234567V",
    "specializations": ["Mathematics", "Physics"],
    "hireDate": "2026-06-09"
  },
  "address": {
    "addressLine1": "No 20",
    "addressLine2": "Temple Road",
    "city": "Kandy",
    "district": "Kandy",
    "postalCode": "20000"
  }
}
```

Expected status:

```txt
201 Created
```

Expected code format:

```txt
teacherProfile.teacherCode = TIT-TCH-001
```

The teacher code is a global teacher sequence:

```txt
TIT-TCH-001
TIT-TCH-002
TIT-TCH-003
```

Teacher specializations are stored separately as a list and do not affect the teacher code.

## 4. Register Admin

Allowed roles:

```txt
SUPER_ADMIN only
```

Request:

```http
POST http://localhost:8080/api/v1/admin/users/admins/register
Authorization: Bearer <access_token>
Content-Type: application/json
```

Body:

```json
{
  "email": "admin1@gmail.com",
  "firstName": "Admin",
  "lastName": "User",
  "phoneNumber": "0773456789",
  "adminProfile": {
    "dateOfBirth": "1992-04-15",
    "nic": "921234567V"
  },
  "address": {
    "addressLine1": "No 30",
    "addressLine2": "Lake Road",
    "city": "Colombo",
    "district": "Colombo",
    "postalCode": "00300"
  }
}
```

Expected status:

```txt
201 Created
```

Expected code format:

```txt
adminProfile.adminCode = TIT-ADM-001
```

The admin code is a global admin sequence:

```txt
TIT-ADM-001
TIT-ADM-002
TIT-ADM-003
```

## 5. Resend Invitation Email

Use this when registration succeeded but the user did not receive the Keycloak password setup email.

Allowed roles:

```txt
ADMIN
SUPER_ADMIN
```

Only users with this local status can receive a resend:

```txt
INVITED
```

Request:

```http
POST http://localhost:8080/api/v1/admin/users/<local-user-id>/resend-invitation
Authorization: Bearer <access_token>
```

No request body is required.

Expected response:

```json
{
  "statusCode": 200,
  "message": "Invitation email sent successfully.",
  "data": null
}
```

## Expected Registration Response Shape

Example response:

```json
{
  "statusCode": 201,
  "message": "User registered successfully. Password setup email will be sent.",
  "data": {
    "id": "local-user-id",
    "keycloakUserId": "keycloak-user-id",
    "email": "student1@gmail.com",
    "firstName": "Kamal",
    "lastName": "Perera",
    "phoneNumber": "0771234567",
    "role": "STUDENT",
    "accountStatus": "INVITED",
    "studentProfile": {
      "studentId": "TIT-07-001",
      "grade": "07"
    },
    "teacherProfile": null,
    "adminProfile": null,
    "address": {
      "addressLine1": "No 10",
      "addressLine2": "Main Street",
      "city": "Colombo",
      "district": "Colombo",
      "postalCode": "00100"
    }
  }
}
```

## Role Access Matrix

| Endpoint | STUDENT | TEACHER | ADMIN | SUPER_ADMIN |
| --- | --- | --- | --- | --- |
| `POST /api/v1/admin/users/register` for STUDENT | 403 | 403 | 201 | 201 |
| `POST /api/v1/admin/users/register` for TEACHER | 403 | 403 | 201 | 201 |
| `POST /api/v1/admin/users/admins/register` | 403 | 403 | 403 | 201 |
| `POST /api/v1/admin/users/{userId}/resend-invitation` | 403 | 403 | 200 | 200 |

Unauthenticated requests should return:

```txt
401 Unauthorized
```

## Validation Checks

Duplicate email:

```txt
Expected: 409 Conflict
Message: Email already exists
```

Invalid role on `/register`, for example `ADMIN`:

```txt
Expected: 400 Bad Request
Message: Only STUDENT and TEACHER users can be registered from this endpoint
```

Missing teacher specializations:

```txt
Expected: 400 Bad Request
Message includes: At least one specialization is required
```

Resend invitation for non-INVITED user:

```txt
Expected: 409 Conflict
Message: Invitation can only be resent for invited users
```

## Database Checks

Useful SQL checks after registration:

```sql
SELECT id, email, role, account_status, keycloak_user_id
FROM users
ORDER BY created_at DESC;
```

```sql
SELECT student_id, grade, first_name, last_name
FROM student_profiles
ORDER BY created_at DESC;
```

```sql
SELECT teacher_code, first_name, last_name
FROM teacher_profiles
ORDER BY created_at DESC;
```

```sql
SELECT teacher_profile_id, specialization
FROM teacher_specializations
ORDER BY teacher_profile_id;
```

```sql
SELECT admin_code, first_name, last_name
FROM admin_profiles
ORDER BY created_at DESC;
```

## Keycloak Checks

After registration, confirm in Keycloak admin console:

```txt
User exists in realm mentify
Email matches request email
Required actions include UPDATE_PASSWORD and VERIFY_EMAIL
Assigned realm role matches local role
```

Expected Keycloak roles:

```txt
STUDENT registration -> STUDENT
TEACHER registration -> TEACHER
ADMIN registration -> ADMIN
```

## Troubleshooting

### 401 Unauthorized

Check:

```txt
Authorization header exists
Token is not expired
Token belongs to realm mentify
user-service issuer-uri points to http://localhost:8180/realms/mentify
```

### 403 Forbidden

Check:

```txt
Token has realm_access.roles
Role converter maps role to ROLE_*
Caller has ADMIN or SUPER_ADMIN depending on endpoint
```

### Keycloak user creation returns 403

The backend confidential client service account probably lacks Keycloak admin permissions.

Required service account role usually includes:

```txt
realm-management manage-users
```

### Password setup email fails

Check Keycloak SMTP realm settings:

```txt
SMTP host/port/user/password configured
From email is configured
Test connection succeeds
User email is valid
```

### Local DB save fails after Keycloak user creation

Expected behavior:

```txt
user-service deletes the newly created Keycloak user as compensation
registration returns database error response
```

### Email fails after DB commit

Expected behavior:

```txt
registration remains committed
admin can call resend invitation endpoint
```

## Maven Verification

Run user-service tests:

```bash
sh cloud/config-server/mvnw -f pom.xml -pl services/user-service -am test
```

Latest local status:

```txt
BUILD SUCCESS
user-service tests passed
```
