# Attendance API Postman Test Guide

Use the API gateway unless you are intentionally testing the service directly.

```txt
base_url = http://localhost:8080
attendance_url = http://localhost:8087
```

Common headers:

```txt
Authorization: Bearer {{teacher_token}} / {{student_token}} / {{admin_token}}
Content-Type: application/json
```

Important ID rule: use `data.user.keycloakUserId` from login as `teacher_id` and `student_id`. Course, enrollment, and attendance services validate against the Keycloak UUID, not the local Mentify `data.user.id`.

Scope: Version 1 covers teacher/admin managed physical-class attendance only. `AttendanceMode` is fixed to `PHYSICAL` for every session created through the API.

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
teacher_id
student_id
course_id
session_id
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

Skip this section if you already have a course assigned to the teacher and the student is already enrolled.

### 4. Create Course

Admin or super admin token required.

```http
POST {{base_url}}/api/v1/course
Authorization: Bearer {{admin_token}}
```

```json
{
  "courseName": "Postman Attendance Course",
  "courseDescription": "Course created for attendance API testing",
  "courseThumbnail": "https://example.com/course.png",
  "courseFeeMonthly": 1000.00,
  "gradeId": "11111111-1111-1111-1111-111111111111",
  "assignedTeacherId": "{{teacher_id}}",
  "subject": "Science",
  "isOnline": false,
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

```http
GET {{base_url}}/api/v1/enrollments/students/{{student_id}}/courses/{{course_id}}/exists
Authorization: Bearer {{student_token}}
```

Expected body:

```json
true
```

## Attendance Session Flow

### 7. Create Attendance Session

Teacher (must be assigned to the course) or admin token required. The service automatically creates one `NOT_MARKED` `AttendanceRecord` per enrolled student.

```http
POST {{base_url}}/api/v1/attendance/sessions
Authorization: Bearer {{teacher_token}}
```

```json
{
  "courseId": "{{course_id}}",
  "title": "Java Programming - Week 04",
  "attendanceDate": "2026-09-11",
  "startTime": "10:00:00",
  "endTime": "12:00:00"
}
```

Postman Tests:

```js
pm.test("session created", () => pm.response.to.have.status(201));
const json = pm.response.json();
pm.environment.set("session_id", json.data.id);
pm.expect(json.data.status).to.eql("OPEN");
pm.expect(json.data.mode).to.eql("PHYSICAL");
pm.expect(json.data.records.every(r => r.status === "NOT_MARKED")).to.be.true;
```

### 8. Create Duplicate Session (Negative Test)

Repeat step 7 with the same `courseId`, `attendanceDate`, and `startTime`.

Expected: `409 Conflict`.

### 9. Create Session As Student (Negative Test)

```http
POST {{base_url}}/api/v1/attendance/sessions
Authorization: Bearer {{student_token}}
```

Use the same body as step 7 (different date/time to avoid the duplicate check).

Expected: `403 Forbidden`.

### 10. List Attendance Sessions For Course

```http
GET {{base_url}}/api/v1/attendance/courses/{{course_id}}/sessions
Authorization: Bearer {{teacher_token}}
```

Expected: `200 OK`, an array of sessions (records omitted in the list view).

### 11. Get Attendance Session By ID

```http
GET {{base_url}}/api/v1/attendance/sessions/{{session_id}}
Authorization: Bearer {{teacher_token}}
```

Expected: `200 OK` with the full `records` array populated.

## Manual Marking Flow

### 12. Mark Attendance (Batch)

```http
PUT {{base_url}}/api/v1/attendance/sessions/{{session_id}}/records
Authorization: Bearer {{teacher_token}}
```

```json
{
  "records": [
    { "studentId": "{{student_id}}", "status": "PRESENT" }
  ]
}
```

Postman Tests:

```js
pm.test("attendance marked", () => pm.response.to.have.status(200));
const json = pm.response.json();
const record = json.data.records.find(r => r.studentId === pm.environment.get("student_id"));
pm.expect(record.status).to.eql("PRESENT");
pm.expect(record.markedBy).to.not.be.undefined;
```

### 13. Mark With Unknown Student (Negative Test)

```http
PUT {{base_url}}/api/v1/attendance/sessions/{{session_id}}/records
Authorization: Bearer {{teacher_token}}
```

```json
{
  "records": [
    { "studentId": "00000000-0000-0000-0000-000000000000", "status": "PRESENT" }
  ]
}
```

Expected: `404 Not Found` (student does not belong to this session).

### 14. Mark With Duplicate Student In Batch (Negative Test)

```json
{
  "records": [
    { "studentId": "{{student_id}}", "status": "PRESENT" },
    { "studentId": "{{student_id}}", "status": "ABSENT" }
  ]
}
```

Expected: `400 Bad Request`.

## Completion And Cancellation Flow

### 15. Complete Session Before All Students Marked (Negative Test)

Create a fresh session (repeat step 7 with a new date) and call complete without marking every student.

```http
POST {{base_url}}/api/v1/attendance/sessions/{{session_id}}/complete
Authorization: Bearer {{teacher_token}}
```

Expected: `409 Conflict`.

### 16. Complete Session

Mark every enrolled student first (step 12), then complete the session.

```http
POST {{base_url}}/api/v1/attendance/sessions/{{session_id}}/complete
Authorization: Bearer {{teacher_token}}
```

Postman Tests:

```js
pm.test("session completed", () => pm.response.to.have.status(200));
const json = pm.response.json();
pm.expect(json.data.status).to.eql("COMPLETED");
```

### 17. Mark Attendance On A Completed Session (Negative Test)

```http
PUT {{base_url}}/api/v1/attendance/sessions/{{session_id}}/records
Authorization: Bearer {{teacher_token}}
```

Expected: `409 Conflict`.

### 18. Cancel Session

Create a new `OPEN` session (repeat step 7) before running this test.

```http
POST {{base_url}}/api/v1/attendance/sessions/{{session_id}}/cancel
Authorization: Bearer {{teacher_token}}
```

Postman Tests:

```js
pm.test("session cancelled", () => pm.response.to.have.status(200));
const json = pm.response.json();
pm.expect(json.data.status).to.eql("CANCELLED");
```

## Attendance Summary Flow

Run these after at least one session for the course has been completed (step 16).

### 19. Get My Attendance Summary (Student)

```http
GET {{base_url}}/api/v1/attendance/courses/{{course_id}}/me
Authorization: Bearer {{student_token}}
```

Postman Tests:

```js
pm.test("my summary ok", () => pm.response.to.have.status(200));
const json = pm.response.json();
pm.expect(json.data.studentId).to.eql(pm.environment.get("student_id"));
pm.expect(json.data.attendancePercentage).to.be.a("number");
```

### 20. Get Student Attendance Summary (Teacher/Admin)

```http
GET {{base_url}}/api/v1/attendance/students/{{student_id}}/courses/{{course_id}}
Authorization: Bearer {{teacher_token}}
```

Expected: `200 OK`, same shape as step 19.

### 21. Get My Attendance Summary As Teacher (Negative Test)

```http
GET {{base_url}}/api/v1/attendance/courses/{{course_id}}/me
Authorization: Bearer {{teacher_token}}
```

Expected: `403 Forbidden` (`/me` is student-only).

### 22. Get Student Summary As Student (Negative Test)

```http
GET {{base_url}}/api/v1/attendance/students/{{student_id}}/courses/{{course_id}}
Authorization: Bearer {{student_token}}
```

Expected: `403 Forbidden`.

## Known Cross-Service Constraint

`course-service`'s `/api/v1/course/{courseId}/lookup` endpoint only permits `TEACHER`, `ADMIN`, and `SUPER_ADMIN` roles. Because of this, `getMyAttendanceSummary` does **not** call that endpoint — a student's `/me` summary is scoped directly by their own JWT `studentId`, with no course-service round trip. If you see a `403` from `course-service` while calling `/me`, confirm the deployed attendance-service build includes this fix.
