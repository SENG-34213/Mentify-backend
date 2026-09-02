# Assignment API Postman Test Guide

Use the API gateway unless you are intentionally testing the service directly.

```txt
base_url = http://localhost:8080
assignment_url = http://localhost:8084
```

Common headers:

```txt
Authorization: Bearer {{teacher_token}} / {{student_token}} / {{admin_token}}
```

For assignment creation and submission creation, use multipart/form-data. The `file` field is optional.

Important ID rule: use `data.user.keycloakUserId` from login as `teacher_id` and `student_id`. The course, enrollment, and assignment services validate against the Keycloak UUID, not the local Mentify `data.user.id`.

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
module_id
assignment_id
submission_id
lesson_id
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
  "courseName": "Postman Assignment Course",
  "courseDescription": "Course created for assignment API testing",
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

### 5. Create Module (if required by your course design)

The assignment service requires a `moduleId` for creation.

```http
POST {{base_url}}/api/v1/modules
Authorization: Bearer {{teacher_token}}
```

```json
{
  "courseId": "{{course_id}}",
  "title": "Assignment Module",
  "description": "Module for assignment testing",
  "orderIndex": 1,
  "isPublished": true
}
```

Postman Tests:

```js
pm.test("module created", () => pm.response.to.have.status(201));
const json = pm.response.json();
pm.environment.set("module_id", json.data.id);
```

### 6. Enroll Student In Course

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

### 7. Verify Enrollment

```http
GET {{base_url}}/api/v1/enrollments/students/{{student_id}}/courses/{{course_id}}/exists
Authorization: Bearer {{student_token}}
```

Expected body:

```json
true
```

## Assignment Management Flow

### 8. Create Assignment

Teacher, admin, or super admin token required.

Important: this endpoint uses `multipart/form-data`, not JSON.

```http
POST {{base_url}}/api/v1/assignments
Authorization: Bearer {{teacher_token}}
Content-Type: multipart/form-data
```

Form-data fields:

```txt
courseId: {{course_id}}
moduleId: {{module_id}}
lessonId: {{lesson_id}}   (optional)
title: Java Fundamentals Assignment
description: Write a short reflection on Java basics
instructions: Submit as a PDF or DOCX and answer all questions.
startDate: 2026-09-02T09:00:00
dueDate: 2026-09-09T09:00:00
maxMarks: 100
allowedAttempts: 2
allowLateSubmission: true
latePenaltyPercentage: 10
file: <optional attachment file>
```

Postman Tests:

```js
pm.test("assignment created", () => pm.response.to.have.status(201));
const json = pm.response.json();
pm.environment.set("assignment_id", json.data.id);
pm.expect(json.data.title).to.eql("Java Fundamentals Assignment");
```

### 9. Update Assignment

```http
PUT {{base_url}}/api/v1/assignments/{{assignment_id}}
Authorization: Bearer {{teacher_token}}
```

```json
{
  "title": "Java Fundamentals Assignment Updated",
  "description": "Updated assignment description",
  "instructions": "Updated instructions. Submit in PDF or DOCX.",
  "startDate": "2026-09-02T09:00:00",
  "dueDate": "2026-09-10T09:00:00",
  "maxMarks": 120,
  "allowedAttempts": 3,
  "allowLateSubmission": true,
  "latePenaltyPercentage": 15
}
```

Expected: `200 OK`.

### 10. Get Assignment By ID

```http
GET {{base_url}}/api/v1/assignments/{{assignment_id}}
Authorization: Bearer {{teacher_token}}
```

Expected: `200 OK`.

### 11. List Assignments For Course

```http
GET {{base_url}}/api/v1/courses/{{course_id}}/assignments
Authorization: Bearer {{student_token}}
```

Expected: `200 OK`.

### 12. Publish Assignment

```http
POST {{base_url}}/api/v1/assignments/{{assignment_id}}/publish
Authorization: Bearer {{teacher_token}}
```

Postman Tests:

```js
pm.test("assignment published", () => pm.response.to.have.status(200));
const json = pm.response.json();
pm.expect(json.data.status).to.eql("PUBLISHED");
```

### 13. Close Assignment

```http
POST {{base_url}}/api/v1/assignments/{{assignment_id}}/close
Authorization: Bearer {{teacher_token}}
```

Expected: `200 OK`.

### 14. Delete Assignment

```http
DELETE {{base_url}}/api/v1/assignments/{{assignment_id}}
Authorization: Bearer {{teacher_token}}
```

Expected: `200 OK`.

## Student Submission Flow

### 15. Create Submission

Student token required.

Important: this endpoint also accepts `multipart/form-data`.

```http
POST {{base_url}}/api/v1/assignments/{{assignment_id}}/submissions
Authorization: Bearer {{student_token}}
Content-Type: multipart/form-data
```

Form-data fields:

```txt
content: This is my assignment answer.
file: <optional submission file>
```

Postman Tests:

```js
pm.test("submission created", () => pm.response.to.have.status(201));
const json = pm.response.json();
pm.environment.set("submission_id", json.data.id);
pm.expect(json.data.status).to.eql("DRAFT");
```

### 16. Update Submission

```http
PUT {{base_url}}/api/v1/submissions/{{submission_id}}
Authorization: Bearer {{student_token}}
```

```json
{
  "content": "Updated submission content for the assignment."
}
```

Expected: `200 OK`.

### 17. Submit Assignment

```http
POST {{base_url}}/api/v1/submissions/{{submission_id}}/submit
Authorization: Bearer {{student_token}}
```

Postman Tests:

```js
pm.test("assignment submitted", () => pm.response.to.have.status(200));
const json = pm.response.json();
pm.expect(json.data.status).to.eql("SUBMITTED");
```

### 18. Get Submission By ID

```http
GET {{base_url}}/api/v1/submissions/{{submission_id}}
Authorization: Bearer {{student_token}}
```

Expected: `200 OK`.

### 19. List Assignments Submissions (Teacher)

```http
GET {{base_url}}/api/v1/assignments/{{assignment_id}}/submissions
Authorization: Bearer {{teacher_token}}
```

Expected: `200 OK`.

### 20. List My Submissions

```http
GET {{base_url}}/api/v1/submissions/my
Authorization: Bearer {{student_token}}
```

Expected: `200 OK`.

## Grading Flow

### 21. Grade Submission

Teacher, admin, or super admin token required.

```http
POST {{base_url}}/api/v1/submissions/{{submission_id}}/grade
Authorization: Bearer {{teacher_token}}
```

```json
{
  "marks": 95,
  "feedback": "Excellent work. You explained the concept clearly and used relevant examples.",
  "status": "GRADED"
}
```

Expected: `200 OK`.

### 22. Return Submission

```http
POST {{base_url}}/api/v1/submissions/{{submission_id}}/return
Authorization: Bearer {{teacher_token}}
```

Expected: `200 OK`.

## Negative Tests

### Student Cannot Create Assignment

```http
POST {{base_url}}/api/v1/assignments
Authorization: Bearer {{student_token}}
```

Use the same multipart fields as the create assignment request.

Expected: `403 Forbidden`.

### Student Not Enrolled

Use a student token whose `student_id` is not enrolled in the course and call:

```http
POST {{base_url}}/api/v1/assignments/{{assignment_id}}/submissions
Authorization: Bearer {{student_token}}
```

Expected: `400 Bad Request` or `403 Forbidden`, message may include:

```txt
Student is not enrolled in this course
```

### Assignment Not Published

Attempt to create a submission for a draft or closed assignment.

Expected: `400 Bad Request`, message may include:

```txt
Assignment is not published yet
```

### Maximum Attempts Reached

Create too many submissions for the assignment after the `allowedAttempts` limit is reached.

Expected: `400 Bad Request`, message may include:

```txt
Maximum submission attempts reached
```

### Late Submission Not Allowed

Try submitting after due date when `allowLateSubmission` is false.

Expected: `400 Bad Request`, message may include:

```txt
Late submission is not allowed for this assignment
```

### Cannot Update Published Assignment

Update an assignment after it has been published.

Expected: `400 Bad Request`, message may include:

```txt
Assignment can only be updated while it is in DRAFT status
```

### Cannot Submit Already Submitted Assignment

Call the submit endpoint twice.

Expected: `400 Bad Request`, message may include:

```txt
Submission has already been submitted
```

## Direct Assignment Service URLs

If bypassing the gateway, replace `{{base_url}}` with `{{assignment_url}}`:

```txt
POST   {{assignment_url}}/api/v1/assignments
PUT    {{assignment_url}}/api/v1/assignments/{{assignment_id}}
GET    {{assignment_url}}/api/v1/assignments/{{assignment_id}}
GET    {{assignment_url}}/api/v1/courses/{{course_id}}/assignments
POST   {{assignment_url}}/api/v1/assignments/{{assignment_id}}/publish
POST   {{assignment_url}}/api/v1/assignments/{{assignment_id}}/close
DELETE {{assignment_url}}/api/v1/assignments/{{assignment_id}}
POST   {{assignment_url}}/api/v1/assignments/{{assignment_id}}/submissions
PUT    {{assignment_url}}/api/v1/submissions/{{submission_id}}
POST   {{assignment_url}}/api/v1/submissions/{{submission_id}}/submit
GET    {{assignment_url}}/api/v1/submissions/{{submission_id}}
GET    {{assignment_url}}/api/v1/assignments/{{assignment_id}}/submissions
GET    {{assignment_url}}/api/v1/submissions/my
POST   {{assignment_url}}/api/v1/submissions/{{submission_id}}/grade
POST   {{assignment_url}}/api/v1/submissions/{{submission_id}}/return
```

## Important Notes

- Assignment creation and submission creation use `multipart/form-data`.
- The `file` field is optional and is used for attachments or upload support.
- Use the same authentication flow as the quiz module.
- A student must be enrolled in the course before they can submit an assignment.
- For file-based assignment scenarios, this is the recommended Postman setup for testing upload handling.
