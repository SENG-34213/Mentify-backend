# Quiz API Postman Test Guide

Use the API gateway unless you are intentionally testing the service directly.

```txt
base_url = http://localhost:8080
assignment_url = http://localhost:8084
```

Common headers:

```txt
Content-Type: application/json
Authorization: Bearer {{teacher_token}} / {{student_token}} / {{admin_token}}
```

Important ID rule: use `data.user.keycloakUserId` from login as `teacher_id` and `student_id`. The quiz, course, and enrollment services validate against the Keycloak UUID, not the local Mentify `data.user.id`.

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
quiz_id
question1_id
question2_id
question1_correct_option_id
question2_correct_option_id
delete_question_id
attempt_id
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
  "courseName": "Postman Quiz Course",
  "courseDescription": "Course created for quiz API testing",
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

```http
GET {{base_url}}/api/v1/enrollments/students/{{student_id}}/courses/{{course_id}}/exists
Authorization: Bearer {{student_token}}
```

Expected body:

```json
true
```

## Quiz Management Flow

### 7. Create Quiz

Teacher, admin, or super admin token required.

```http
POST {{base_url}}/api/quizzes
Authorization: Bearer {{teacher_token}}
```

```json
{
  "courseId": "{{course_id}}",
  "title": "Java Basics Quiz",
  "description": "Multiple choice quiz for API testing",
  "durationMinutes": 30,
  "passMark": 2.00,
  "startTime": null,
  "endTime": null,
  "maxAttempts": 2,
  "showResultImmediately": true
}
```

Postman Tests:

```js
pm.test("quiz created", () => pm.response.to.have.status(201));
const json = pm.response.json();
pm.environment.set("quiz_id", json.data.id);
pm.expect(json.data.status).to.eql("DRAFT");
```

### 8. Update Quiz While Draft

```http
PUT {{base_url}}/api/quizzes/{{quiz_id}}
Authorization: Bearer {{teacher_token}}
```

```json
{
  "title": "Java Basics Quiz Updated",
  "description": "Updated quiz metadata",
  "durationMinutes": 35,
  "passMark": 2.00,
  "startTime": null,
  "endTime": null,
  "maxAttempts": 2,
  "showResultImmediately": true
}
```

Expected: `200 OK`.

### 9. Create Question 1

```http
POST {{base_url}}/api/quizzes/{{quiz_id}}/questions
Authorization: Bearer {{teacher_token}}
```

```json
{
  "questionText": "Which keyword is used to inherit a class in Java?",
  "questionType": "MULTIPLE_CHOICE_SINGLE_ANSWER",
  "marks": 1.00,
  "questionOrder": 1,
  "options": [
    { "optionText": "implement", "correct": false, "optionOrder": 1 },
    { "optionText": "extends", "correct": true, "optionOrder": 2 },
    { "optionText": "inherit", "correct": false, "optionOrder": 3 },
    { "optionText": "super", "correct": false, "optionOrder": 4 }
  ]
}
```

Postman Tests:

```js
pm.test("question 1 created", () => pm.response.to.have.status(201));
const json = pm.response.json();
pm.environment.set("question1_id", json.data.id);
const correct = json.data.options.find(o => o.correct === true);
pm.environment.set("question1_correct_option_id", correct.id);
```

### 10. Create Question 2

```http
POST {{base_url}}/api/quizzes/{{quiz_id}}/questions
Authorization: Bearer {{teacher_token}}
```

```json
{
  "questionText": "Which collection type stores unique values in Java?",
  "questionType": "MULTIPLE_CHOICE_SINGLE_ANSWER",
  "marks": 1.00,
  "questionOrder": 2,
  "options": [
    { "optionText": "List", "correct": false, "optionOrder": 1 },
    { "optionText": "Map", "correct": false, "optionOrder": 2 },
    { "optionText": "Set", "correct": true, "optionOrder": 3 },
    { "optionText": "Queue", "correct": false, "optionOrder": 4 }
  ]
}
```

Postman Tests:

```js
pm.test("question 2 created", () => pm.response.to.have.status(201));
const json = pm.response.json();
pm.environment.set("question2_id", json.data.id);
const correct = json.data.options.find(o => o.correct === true);
pm.environment.set("question2_correct_option_id", correct.id);
```

### 11. Update Question

```http
PUT {{base_url}}/api/quizzes/{{quiz_id}}/questions/{{question2_id}}
Authorization: Bearer {{teacher_token}}
```

```json
{
  "questionText": "Which Java collection stores unique elements?",
  "questionType": "MULTIPLE_CHOICE_SINGLE_ANSWER",
  "marks": 1.00,
  "questionOrder": 2,
  "options": [
    { "optionText": "ArrayList", "correct": false, "optionOrder": 1 },
    { "optionText": "HashMap", "correct": false, "optionOrder": 2 },
    { "optionText": "HashSet", "correct": true, "optionOrder": 3 },
    { "optionText": "LinkedList", "correct": false, "optionOrder": 4 }
  ]
}
```

Postman Tests:

```js
pm.test("question updated", () => pm.response.to.have.status(200));
const json = pm.response.json();
const correct = json.data.options.find(o => o.correct === true);
pm.environment.set("question2_correct_option_id", correct.id);
```

### 12. Get Teacher Quiz

```http
GET {{base_url}}/api/quizzes/{{quiz_id}}
Authorization: Bearer {{teacher_token}}
```

Expected: `200 OK`. Response includes correct answer flags in `data.questions[].options[].correct`.

### 13. Create Question To Delete

This covers the delete endpoint without removing the two questions used for scoring.

```http
POST {{base_url}}/api/quizzes/{{quiz_id}}/questions
Authorization: Bearer {{teacher_token}}
```

```json
{
  "questionText": "Temporary question for delete test",
  "questionType": "MULTIPLE_CHOICE_SINGLE_ANSWER",
  "marks": 1.00,
  "questionOrder": 99,
  "options": [
    { "optionText": "Option A", "correct": true, "optionOrder": 1 },
    { "optionText": "Option B", "correct": false, "optionOrder": 2 },
    { "optionText": "Option C", "correct": false, "optionOrder": 3 },
    { "optionText": "Option D", "correct": false, "optionOrder": 4 }
  ]
}
```

Postman Tests:

```js
pm.test("delete test question created", () => pm.response.to.have.status(201));
const json = pm.response.json();
pm.environment.set("delete_question_id", json.data.id);
```

### 14. Delete Question

```http
DELETE {{base_url}}/api/quizzes/{{quiz_id}}/questions/{{delete_question_id}}
Authorization: Bearer {{teacher_token}}
```

Expected: `200 OK`.

### 15. Publish Quiz

```http
POST {{base_url}}/api/quizzes/{{quiz_id}}/publish
Authorization: Bearer {{teacher_token}}
```

Postman Tests:

```js
pm.test("quiz published", () => pm.response.to.have.status(200));
const json = pm.response.json();
pm.expect(json.data.status).to.eql("PUBLISHED");
pm.expect(Number(json.data.totalMarks)).to.eql(2);
```

## Student Quiz Flow

### 16. List Published Quizzes For Course

```http
GET {{base_url}}/api/courses/{{course_id}}/quizzes
Authorization: Bearer {{student_token}}
```

Expected: `200 OK`. Response does not include correct flags.

### 17. Get Student Quiz Details

```http
GET {{base_url}}/api/quizzes/{{quiz_id}}/student
Authorization: Bearer {{student_token}}
```

Expected: `200 OK`. Response includes questions and options, but no `correct` fields.

### 18. Start Quiz Attempt

```http
POST {{base_url}}/api/quizzes/{{quiz_id}}/attempts
Authorization: Bearer {{student_token}}
```

Postman Tests:

```js
pm.test("attempt started", () => pm.response.to.have.status(201));
const json = pm.response.json();
pm.environment.set("attempt_id", json.data.attemptId);
pm.expect(json.data.status).to.eql("IN_PROGRESS");
```

### 19. Save Answer 1

```http
PUT {{base_url}}/api/quiz-attempts/{{attempt_id}}/answers
Authorization: Bearer {{student_token}}
```

```json
{
  "questionId": "{{question1_id}}",
  "selectedOptionId": "{{question1_correct_option_id}}"
}
```

Expected: `200 OK`.

### 20. Save Answer 2

```http
PUT {{base_url}}/api/quiz-attempts/{{attempt_id}}/answers
Authorization: Bearer {{student_token}}
```

```json
{
  "questionId": "{{question2_id}}",
  "selectedOptionId": "{{question2_correct_option_id}}"
}
```

Expected: `200 OK`.

### 21. Submit Attempt

```http
POST {{base_url}}/api/quiz-attempts/{{attempt_id}}/submit
Authorization: Bearer {{student_token}}
```

Postman Tests:

```js
pm.test("attempt submitted", () => pm.response.to.have.status(200));
const json = pm.response.json();
pm.expect(Number(json.data.score)).to.eql(2);
pm.expect(Number(json.data.totalMarks)).to.eql(2);
pm.expect(json.data.passed).to.eql(true);
```

## Negative Tests

### Student Cannot Create Quiz

```http
POST {{base_url}}/api/quizzes
Authorization: Bearer {{student_token}}
```

Use the same create quiz body. Expected: `403 Forbidden`.

### Cannot Publish Empty Quiz

Create a new quiz and publish it before adding questions.

```http
POST {{base_url}}/api/quizzes/{{quiz_id}}/publish
Authorization: Bearer {{teacher_token}}
```

Expected: `400 Bad Request`, message:

```txt
Quiz must contain at least one question before publishing
```

### Invalid Question: Only 3 Options

```http
POST {{base_url}}/api/quizzes/{{quiz_id}}/questions
Authorization: Bearer {{teacher_token}}
```

```json
{
  "questionText": "Invalid question",
  "questionType": "MULTIPLE_CHOICE_SINGLE_ANSWER",
  "marks": 1.00,
  "questionOrder": 99,
  "options": [
    { "optionText": "A", "correct": true, "optionOrder": 1 },
    { "optionText": "B", "correct": false, "optionOrder": 2 },
    { "optionText": "C", "correct": false, "optionOrder": 3 }
  ]
}
```

Expected: `400 Bad Request`, message:

```txt
A question must contain between 4 and 5 options
```

### Invalid Question: Multiple Correct Answers

Use 4 options with two `"correct": true`. Expected: `400 Bad Request`, message:

```txt
Exactly one correct answer is required
```

### Invalid Question: Duplicate Options

Use duplicate option text such as `"A"` twice. Expected: `400 Bad Request`, message:

```txt
Duplicate option text is not allowed within the same question
```

### Cannot Edit Published Quiz

After publishing, call update quiz or create/update/delete question.

Expected: `400 Bad Request`. Possible messages:

```txt
Quiz can only be edited while it is in DRAFT status
Questions can only be edited while the quiz is in DRAFT status
```

### Cannot Start Draft Quiz

Create a quiz but do not publish it. Then start attempt as student.

```http
POST {{base_url}}/api/quizzes/{{quiz_id}}/attempts
Authorization: Bearer {{student_token}}
```

Expected: `400 Bad Request`, message:

```txt
Only published quizzes can be attempted
```

### Cannot Start Attempt Twice

Start an attempt, but do not submit it. Call start attempt again.

Expected: `409 Conflict`, message:

```txt
Student already has an active attempt for this quiz
```

### Cannot Save Answer After Submit

Submit an attempt, then call save answer again.

Expected: `409 Conflict`, message:

```txt
Quiz attempt has already been submitted
```

### Cannot Submit Twice

Submit an attempt twice.

Expected: `409 Conflict`, message:

```txt
Quiz attempt has already been submitted
```

### Student Not Enrolled

Use a student token whose `student_id` is not enrolled in the course and call:

```http
GET {{base_url}}/api/courses/{{course_id}}/quizzes
POST {{base_url}}/api/quizzes/{{quiz_id}}/attempts
```

Expected: `403 Forbidden`, message:

```txt
Student is not enrolled in this course
```

### Attempt Limit Exceeded

Set `"maxAttempts": 1`, start an attempt, submit it, then try to start again.

Expected: `409 Conflict`, message:

```txt
Quiz attempt limit has been exceeded
```

## Direct Assignment Service URLs

If bypassing the gateway, replace `{{base_url}}` with `{{assignment_url}}` for quiz endpoints only:

```txt
POST   {{assignment_url}}/api/quizzes
PUT    {{assignment_url}}/api/quizzes/{{quiz_id}}
GET    {{assignment_url}}/api/quizzes/{{quiz_id}}
POST   {{assignment_url}}/api/quizzes/{{quiz_id}}/publish
POST   {{assignment_url}}/api/quizzes/{{quiz_id}}/questions
PUT    {{assignment_url}}/api/quizzes/{{quiz_id}}/questions/{{question_id}}
DELETE {{assignment_url}}/api/quizzes/{{quiz_id}}/questions/{{question_id}}
GET    {{assignment_url}}/api/courses/{{course_id}}/quizzes
GET    {{assignment_url}}/api/quizzes/{{quiz_id}}/student
POST   {{assignment_url}}/api/quizzes/{{quiz_id}}/attempts
PUT    {{assignment_url}}/api/quiz-attempts/{{attempt_id}}/answers
POST   {{assignment_url}}/api/quiz-attempts/{{attempt_id}}/submit
```
