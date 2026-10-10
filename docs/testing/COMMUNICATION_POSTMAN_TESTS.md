# Communication API Postman Test Guide

Use the API gateway unless you are intentionally testing the service directly.

```txt
base_url = http://localhost:8080
communication_direct_url = http://localhost:8086
```

Common headers:

```txt
Content-Type: application/json
Authorization: Bearer {{teacher_token}} / {{student_token}} / {{admin_token}}
```

Important ID rule: use `data.user.keycloakUserId` from login as `teacher_id`, `student_id`, and `admin_id`. The communication service reads the current user from the JWT subject and stores group member IDs as Keycloak UUIDs.

## Environment Variables

Create these Postman environment variables:

```txt
base_url
admin_email
admin_password
teacher_email
teacher_password
student_email
student_password
admin_token
teacher_token
student_token
admin_id
teacher_id
student_id
course_id
communication_group_id
unassigned_teacher_token
unassigned_teacher_id
```

## Auth Setup

### 1. Login as Admin

```http
POST {{base_url}}/api/v1/auth/login
```

```json
{
  "identifier": "{{admin_email}}",
  "password": "{{admin_password}}"
}
```

Postman Tests:

```js
pm.test("admin login ok", () => pm.response.to.have.status(200));
const json = pm.response.json();
pm.environment.set("admin_token", json.data.accessToken);
pm.environment.set("admin_id", json.data.user.keycloakUserId);
```

### 2. Login as Teacher

```http
POST {{base_url}}/api/v1/auth/login
```

```json
{
  "identifier": "{{teacher_email}}",
  "password": "{{teacher_password}}"
}
```

Postman Tests:

```js
pm.test("teacher login ok", () => pm.response.to.have.status(200));
const json = pm.response.json();
pm.environment.set("teacher_token", json.data.accessToken);
pm.environment.set("teacher_id", json.data.user.keycloakUserId);
```

### 3. Login as Student

```http
POST {{base_url}}/api/v1/auth/login
```

```json
{
  "identifier": "{{student_email}}",
  "password": "{{student_password}}"
}
```

Postman Tests:

```js
pm.test("student login ok", () => pm.response.to.have.status(200));
const json = pm.response.json();
pm.environment.set("student_token", json.data.accessToken);
pm.environment.set("student_id", json.data.user.keycloakUserId);
```

## Required Setup

Skip this section if you already have a published course assigned to the teacher and the student is already enrolled.

### 4. Create Course

Admin or super admin token required.

```http
POST {{base_url}}/api/v1/course
Authorization: Bearer {{admin_token}}
```

```json
{
  "courseName": "Postman Communication Course",
  "courseDescription": "Course created for communication API testing",
  "courseThumbnail": "https://example.com/course.png",
  "courseFeeMonthly": 1000.00,
  "gradeId": "11111111-1111-1111-1111-111111111111",
  "assignedTeacherId": "{{teacher_id}}",
  "subject": "Science",
  "isOnline": true,
  "discountOfferPercent": 0,
  "isVisible": true,
  "isPublished": true
}
```

Postman Tests:

```js
pm.test("course created", () => pm.response.to.have.status(201));
const json = pm.response.json();
pm.environment.set("course_id", json.data.id);
```

### 5. Enroll Student In Course

Admin or super admin token required.

```http
POST {{base_url}}/api/v1/enrollments
Authorization: Bearer {{admin_token}}
```

```json
{
  "studentId": "{{student_id}}",
  "courseIds": ["{{course_id}}"]
}
```

Expected: `201 Created`.

### 6. Verify Enrollment

Student, teacher, admin, or super admin token required.

```http
GET {{base_url}}/api/v1/enrollments/students/{{student_id}}/courses/{{course_id}}/exists
Authorization: Bearer {{student_token}}
```

Expected body:

```json
true
```

## Communication Group Flow

### 7. Authenticated User Probe

Use this to confirm the communication service can parse the JWT subject.

```http
GET {{base_url}}/api/communication/me
Authorization: Bearer {{teacher_token}}
```

Expected body:

```json
{
  "userId": "{{teacher_id}}"
}
```

Postman Tests:

```js
pm.test("me endpoint ok", () => pm.response.to.have.status(200));
const json = pm.response.json();
pm.expect(json.userId).to.eql(pm.environment.get("teacher_id"));
```

### 8. Create Communication Group

Teacher, admin, or super admin token required.

When a group is created, the service:

```txt
Validates that the course exists through course-service
Requires a teacher creator to be assigned to the course
Allows only one communication group per course
Adds the creator as ADMIN or TEACHER
Adds the assigned course teacher as TEACHER
Adds currently enrolled students as STUDENT members
```

```http
POST {{base_url}}/api/communication/groups
Authorization: Bearer {{teacher_token}}
Content-Type: application/json
```

```json
{
  "courseId": "{{course_id}}",
  "name": "Postman Communication Group",
  "description": "Official communication group for Postman testing"
}
```

Expected: `201 Created`.

Expected response shape:

```json
{
  "statusCode": 201,
  "message": "Communication group created successfully",
  "data": {
    "id": "00000000-0000-0000-0000-000000000000",
    "courseId": "{{course_id}}",
    "name": "Postman Communication Group",
    "description": "Official communication group for Postman testing",
    "status": "ACTIVE"
  }
}
```

Postman Tests:

```js
pm.test("communication group created", () => pm.response.to.have.status(201));
const json = pm.response.json();
pm.expect(json.message).to.eql("Communication group created successfully");
pm.expect(json.data.courseId).to.eql(pm.environment.get("course_id"));
pm.expect(json.data.status).to.eql("ACTIVE");
pm.environment.set("communication_group_id", json.data.id);
```

### 9. List My Communication Groups

Any authenticated active group member can call this endpoint.

```http
GET {{base_url}}/api/communication/groups
Authorization: Bearer {{teacher_token}}
```

Expected: `200 OK`.

Expected response shape:

```json
{
  "statusCode": 200,
  "message": "Communication groups fetched successfully",
  "data": [
    {
      "id": "{{communication_group_id}}",
      "courseId": "{{course_id}}",
      "name": "Postman Communication Group",
      "description": "Official communication group for Postman testing",
      "status": "ACTIVE"
    }
  ]
}
```

Postman Tests:

```js
pm.test("groups fetched", () => pm.response.to.have.status(200));
const json = pm.response.json();
pm.expect(json.message).to.eql("Communication groups fetched successfully");
pm.expect(json.data).to.be.an("array");
pm.expect(json.data.some(group => group.id === pm.environment.get("communication_group_id"))).to.eql(true);
```

### 10. Get Communication Group By ID

Only active members of the group can retrieve it.

```http
GET {{base_url}}/api/communication/groups/{{communication_group_id}}
Authorization: Bearer {{teacher_token}}
```

Expected: `200 OK`.

Postman Tests:

```js
pm.test("group fetched", () => pm.response.to.have.status(200));
const json = pm.response.json();
pm.expect(json.message).to.eql("Communication group fetched successfully");
pm.expect(json.data.id).to.eql(pm.environment.get("communication_group_id"));
pm.expect(json.data.status).to.eql("ACTIVE");
```

### 11. Get Communication Group Members

Only active members of the group can retrieve the member list.

```http
GET {{base_url}}/api/communication/groups/{{communication_group_id}}/members
Authorization: Bearer {{teacher_token}}
```

Expected: `200 OK`.

Expected response shape:

```json
{
  "statusCode": 200,
  "message": "Group members fetched successfully",
  "data": [
    {
      "userId": "{{teacher_id}}",
      "role": "TEACHER",
      "joinedAt": "2026-09-04T10:30:00"
    },
    {
      "userId": "{{student_id}}",
      "role": "STUDENT",
      "joinedAt": "2026-09-04T10:30:00"
    }
  ]
}
```

Postman Tests:

```js
pm.test("members fetched", () => pm.response.to.have.status(200));
const json = pm.response.json();
pm.expect(json.message).to.eql("Group members fetched successfully");
pm.expect(json.data).to.be.an("array");
pm.expect(json.data.some(member => member.userId === pm.environment.get("teacher_id"))).to.eql(true);
pm.expect(json.data.some(member => member.userId === pm.environment.get("student_id"))).to.eql(true);
```

### 12. Student Lists Communication Groups

This verifies that enrolled students were seeded as members when the group was created.

```http
GET {{base_url}}/api/communication/groups
Authorization: Bearer {{student_token}}
```

Expected: `200 OK`.

Postman Tests:

```js
pm.test("student can see group", () => pm.response.to.have.status(200));
const json = pm.response.json();
pm.expect(json.data.some(group => group.id === pm.environment.get("communication_group_id"))).to.eql(true);
```

### 13. Student Gets Communication Group Members

```http
GET {{base_url}}/api/communication/groups/{{communication_group_id}}/members
Authorization: Bearer {{student_token}}
```

Expected: `200 OK`.

Postman Tests:

```js
pm.test("student can fetch group members", () => pm.response.to.have.status(200));
const json = pm.response.json();
pm.expect(json.data.some(member => member.userId === pm.environment.get("student_id"))).to.eql(true);
```

### 14. Archive Communication Group

Admin, super admin, or an active teacher member can archive a group. Student members cannot archive.

```http
POST {{base_url}}/api/communication/groups/{{communication_group_id}}/archive
Authorization: Bearer {{teacher_token}}
```

Expected: `200 OK`.

Expected response shape:

```json
{
  "statusCode": 200,
  "message": "Communication group archived successfully",
  "data": {
    "id": "{{communication_group_id}}",
    "courseId": "{{course_id}}",
    "name": "Postman Communication Group",
    "description": "Official communication group for Postman testing",
    "status": "ARCHIVED"
  }
}
```

Postman Tests:

```js
pm.test("group archived", () => pm.response.to.have.status(200));
const json = pm.response.json();
pm.expect(json.message).to.eql("Communication group archived successfully");
pm.expect(json.data.status).to.eql("ARCHIVED");
```

### 15. Archived Group Is Hidden From List

```http
GET {{base_url}}/api/communication/groups
Authorization: Bearer {{teacher_token}}
```

Expected: `200 OK`, and the archived group should not appear in `data`.

Postman Tests:

```js
pm.test("archived group is hidden", () => pm.response.to.have.status(200));
const json = pm.response.json();
pm.expect(json.data.some(group => group.id === pm.environment.get("communication_group_id"))).to.eql(false);
```

### 16. Archived Group Cannot Be Retrieved By ID

```http
GET {{base_url}}/api/communication/groups/{{communication_group_id}}
Authorization: Bearer {{teacher_token}}
```

Expected: `404 Not Found`.

Expected error shape:

```json
{
  "statusCode": 404,
  "message": "Communication group not found with id: '{{communication_group_id}}'",
  "path": "/api/communication/groups/{{communication_group_id}}",
  "timestamp": "2026-09-04 10:30:00"
}
```

Postman Tests:

```js
pm.test("archived group returns not found", () => pm.response.to.have.status(404));
const json = pm.response.json();
pm.expect(json.statusCode).to.eql(404);
```

## Negative Tests

Run negative tests that create or archive groups before the archive step, because archived groups are no longer active.

### Missing Course ID

```http
POST {{base_url}}/api/communication/groups
Authorization: Bearer {{teacher_token}}
Content-Type: application/json
```

```json
{
  "name": "Invalid Communication Group",
  "description": "Missing course ID"
}
```

Expected: `400 Bad Request`, message:

```txt
Course ID is required
```

### Missing Group Name

```http
POST {{base_url}}/api/communication/groups
Authorization: Bearer {{teacher_token}}
Content-Type: application/json
```

```json
{
  "courseId": "{{course_id}}",
  "description": "Missing name"
}
```

Expected: `400 Bad Request`, message:

```txt
Group name is required
```

### Group Name Too Long

Use a `name` longer than 150 characters.

Expected: `400 Bad Request`, message:

```txt
Group name must not exceed 150 characters
```

### Student Cannot Create Communication Group

```http
POST {{base_url}}/api/communication/groups
Authorization: Bearer {{student_token}}
Content-Type: application/json
```

```json
{
  "courseId": "{{course_id}}",
  "name": "Student Created Group",
  "description": "Students are not allowed to create groups"
}
```

Expected: `403 Forbidden`.

Possible message:

```txt
Access denied
```

### Duplicate Group For Same Course

Run `Create Communication Group` twice with the same `courseId`.

Expected: `409 Conflict`, message:

```txt
Communication group already exists for course: '{{course_id}}'
```

### Teacher Not Assigned To Course

Use a teacher token whose Keycloak user ID does not match the course `assignedTeacherId`.

```http
POST {{base_url}}/api/communication/groups
Authorization: Bearer {{unassigned_teacher_token}}
Content-Type: application/json
```

```json
{
  "courseId": "{{course_id}}",
  "name": "Unassigned Teacher Group",
  "description": "Teacher is not assigned to this course"
}
```

Expected: `403 Forbidden`, message:

```txt
Teacher '{{unassigned_teacher_id}}' is not assigned to course '{{course_id}}'
```

### Invalid Course

```http
POST {{base_url}}/api/communication/groups
Authorization: Bearer {{teacher_token}}
Content-Type: application/json
```

```json
{
  "courseId": "00000000-0000-0000-0000-000000000000",
  "name": "Missing Course Group",
  "description": "Course does not exist"
}
```

Expected: `404 Not Found`, message:

```txt
Course not found with id: '00000000-0000-0000-0000-000000000000'
```

### Non-Member Cannot Fetch Group

Use a valid token for a user who is not a seeded member of `communication_group_id`.

```http
GET {{base_url}}/api/communication/groups/{{communication_group_id}}
Authorization: Bearer {{unassigned_teacher_token}}
```

Expected: `403 Forbidden`, message:

```txt
You are not an active member of this communication group
```

### Student Cannot Archive Group

```http
POST {{base_url}}/api/communication/groups/{{communication_group_id}}/archive
Authorization: Bearer {{student_token}}
```

Expected: `403 Forbidden`.

Possible message:

```txt
Access denied
```

### Missing Token

```http
GET {{base_url}}/api/communication/me
```

Expected: `401 Unauthorized`.

## Direct Service Note

The API gateway routes `/api/communication/**` to `communication-service`. To bypass the gateway during local debugging, replace `{{base_url}}` with `{{communication_direct_url}}`.
