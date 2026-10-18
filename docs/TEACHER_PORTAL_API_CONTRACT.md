# Teacher Portal Backend Contract

This document records the current controller contract for the teacher-portal services.
Protected endpoints require a bearer JWT. Role names below are the controller's
`@PreAuthorize` rules; course ownership, enrollment scope, and other service-level
checks also apply. Unless noted, successful JSON responses use the shared
`ApiResponse` envelope.
When a route shows an optional item segment (for example,
`/resources[/{resourceId}]`), the collection and item paths are separate
routes; the listed HTTP methods apply to the appropriate path variant.

## Courses and course content (`course-service`)

### Teacher student directory

| Method | Path | Roles | Result |
|---|---|---|---|
| GET | `/api/v1/teacher/students?courseId={courseId}` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | Student directory entries |

The course service verifies that a teacher owns the requested course before
reading its active enrollment IDs. It then resolves the IDs through the user
service and returns name, email, student number, account status, and the course
scope in the standard `ApiResponse` envelope. Avatar and program are currently
returned as `null` because those fields are not persisted in `student_profiles`.

| Method | Path | Roles | Request / result |
|---|---|---|---|
| GET | `/api/v1/course/my-courses` | `TEACHER` | Teacher's assigned courses |
| POST | `/api/v1/course` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | `CourseRequest` |
| PUT | `/api/v1/course/{courseId}` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | `CourseRequest` |
| GET | `/api/v1/course/{courseId}` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | One course |
| GET | `/api/v1/course` | `ADMIN`, `SUPER_ADMIN` | All courses |
| GET | `/api/v1/course/{courseId}/lookup` | `STUDENT`, `TEACHER`, `ADMIN`, `SUPER_ADMIN` | Course lookup |
| POST | `/api/v1/course/bulk` | `ADMIN`, `SUPER_ADMIN` | `CourseBulkLookupRequest` |
| DELETE | `/api/v1/course/{courseId}` | `ADMIN`, `SUPER_ADMIN` | Delete course |
| POST, PUT | `/api/v1/course/{courseId}/modules[/{moduleId}]` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | `ModuleCreateRequest` |
| GET, DELETE | `/api/v1/course/{courseId}/modules[/{moduleId}]` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | List, fetch, or delete modules |
| POST, PUT | `/api/v1/course/{courseId}/modules/{moduleId}/lessons[/{lessonId}]` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | `LessonCreateRequest` |
| GET, DELETE | `/api/v1/course/{courseId}/modules/{moduleId}/lessons[/{lessonId}]` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | List, fetch, or delete lessons |
| POST, PUT, DELETE | `/api/v1/course/{courseId}/modules/{moduleId}/learning-materials[/{materialId}]` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | `LearningMaterialCreateRequest` for POST/PUT |
| GET | `/api/v1/course/{courseId}/modules/{moduleId}/learning-materials[/{materialId}]` | `STUDENT`, `TEACHER`, `ADMIN`, `SUPER_ADMIN` | List or fetch attached materials |

The optional path segments in brackets denote distinct collection and item
endpoints. `CourseRequest` requires nonblank `courseName` (max 150),
`courseDescription` (max 2000), and `subject` (max 100), plus non-null `gradeId`
and `assignedTeacherId`. `ModuleCreateRequest` requires a nonblank title (max
150) and `sequenceOrder >= 1`; optional description is max 2000 and
`dateDuration`, when supplied, is at least 1. `LessonCreateRequest` requires a
nonblank title (max 150); optional description is max 2000. Attached material
requires a nonblank title (max 150); optional `fileUrl` is max 500. Teacher
operations are additionally checked against course ownership by the course
service.

### Grade catalog (`course-service`)

All endpoints below require `ADMIN` or `SUPER_ADMIN`.

| Method | Path | Request / result |
|---|---|---|
| GET | `/api/v1/course/grades` | List grades |
| POST | `/api/v1/course/grades` | `GradeRequest` |
| GET | `/api/v1/course/grades/{gradeId}` | Fetch a grade |
| PUT | `/api/v1/course/grades/{gradeId}` | `GradeRequest` |
| DELETE | `/api/v1/course/grades/{gradeId}` | Delete a grade |
| GET | `/api/v1/course/grades/monthly-fees` | List monthly fees |
| GET, POST, PUT, DELETE | `/api/v1/course/grades/{gradeId}/monthly-fee` | Read, create, update, or delete fee |

`GradeRequest` requires a nonblank `name` (max 100) and `monthlyFee` greater
than or equal to `0.01`, with at most 10 integer and 2 fractional digits.
Optional `description` is max 1000; `currencyCode` defaults to `LKR` and, when
provided, must be a three-letter code. Monthly-fee create/update requests use
`GradeMonthlyFeeRequest` and apply the same fee and currency constraints.

## Enrollment (`entrollment-service`)

| Method | Path | Roles | Request / result |
|---|---|---|---|
| GET | `/api/v1/enrollments` | `ADMIN`, `SUPER_ADMIN` | Active enrollments |
| POST | `/api/v1/enrollments` | `ADMIN`, `SUPER_ADMIN` | `EntrollmentCreateRequest` |
| POST | `/api/v1/enrollments/me` | `STUDENT` | `StudentEntrollmentCreateRequest`; student identity comes from JWT |
| PUT | `/api/v1/enrollments/{enrollmentId}` | `ADMIN`, `SUPER_ADMIN` | `EntrollmentUpdateRequest` |
| GET | `/api/v1/enrollments/students/{studentId}/courses/{courseId}/exists` | `STUDENT`, `TEACHER`, `ADMIN`, `SUPER_ADMIN` | Boolean |
| GET | `/api/v1/enrollments/courses/{courseId}/students` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | Enrolled student IDs |
| GET | `/api/v1/enrollments/students/{studentId}/courses` | `STUDENT` | Active course IDs |
| GET | `/api/v1/enrollments/students/unenrolled` | `ADMIN`, `SUPER_ADMIN` | Unenrolled students |

Admin create enrollment accepts a non-null `studentId` and a non-empty list of
`courseIds`. Self-enrollment accepts a non-empty `courseIds` list and does not
accept the acting student's ID in its request body. Update also requires a
non-empty course list. Paid-course enrollment is contingent on successful
payment verification.

## Assignments, grading, and quizzes (`assignment-service`)

| Method | Path | Roles | Request / result |
|---|---|---|---|
| POST, PUT, PATCH | `/api/v1/assignments[/{assignmentId}]` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | `CreateAssignmentRequest`, full `UpdateAssignmentRequest`, or sparse `PatchAssignmentRequest` |
| GET | `/api/v1/assignments/{assignmentId}` | `TEACHER`, `ADMIN`, `SUPER_ADMIN`, `STUDENT` | Assignment, rubric items, total submission count, and graded count |
| GET | `/api/v1/courses/{courseId}/assignments` | `TEACHER`, `ADMIN`, `SUPER_ADMIN`, `STUDENT` | Course assignments with rubric items and submission counts |
| DELETE | `/api/v1/assignments/{assignmentId}` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | Delete assignment |
| POST | `/api/v1/assignments/{assignmentId}/publish` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | Publish assignment |
| POST | `/api/v1/assignments/{assignmentId}/close` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | Close assignment |
| GET | `/api/v1/assignments/{assignmentId}/submissions` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | List submissions including student names |
| POST | `/api/v1/submissions/{submissionId}/grade` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | `GradeSubmissionRequest` |
| POST | `/api/v1/submissions/{submissionId}/return` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | Return graded submission |
| POST | `/api/v1/assignments/{assignmentId}/attachments` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | `AttachAssignmentMaterialRequest` |
| GET | `/api/v1/assignments/{assignmentId}/attachments` | `STUDENT`, `TEACHER`, `ADMIN`, `SUPER_ADMIN` | List attached materials |
| GET | `/api/v1/assignments/{assignmentId}/attachments/{attachmentId}/download` | `STUDENT`, `TEACHER`, `ADMIN`, `SUPER_ADMIN` | Download URL |
| DELETE | `/api/v1/assignments/{assignmentId}/attachments/{attachmentId}` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | Remove attachment |
| POST, PUT | `/api/quizzes[/{quizId}]` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | `CreateQuizRequest`, `UpdateQuizRequest` |
| GET | `/api/quizzes/teacher`, `/api/quizzes/{quizId}` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | Teacher quiz list or detail |
| DELETE | `/api/quizzes/{quizId}` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | Delete quiz |
| POST | `/api/quizzes/{quizId}/publish` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | Publish quiz |
| POST, PUT | `/api/quizzes/{quizId}/questions[/{questionId}]` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | `CreateQuestionRequest` |
| DELETE | `/api/quizzes/{quizId}/questions/{questionId}` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | Delete question |
| GET | `/api/internal/quizzes/analytics/today/performance` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | `TodayQuizPerformanceResponse` |

Assignment creation requires `courseId`, `moduleId`, title (nonblank, max
150), `startDate`, `dueDate`, `maxMarks >= 1`, `allowedAttempts >= 1`, and
`allowLateSubmission`; full update requires the same editable fields. Both
requests may include up to 50 `rubricItems`, each with a nonblank `criterion`
(max 150) and nonnegative `marks`; the sum of rubric marks cannot exceed
`maxMarks`. Assignment responses include ordered rubric items and
`totalSubmissions`/`gradedCount`; graded counts include submissions with saved
marks. `PATCH /api/v1/assignments/{assignmentId}` accepts any subset of
editable assignment fields and preserves fields omitted from the request; an
empty patch is rejected. A supplied rubric array replaces the current rubric,
while an omitted rubric leaves it unchanged. Late penalty, if supplied, is
0–100.

The user service exposes `POST /api/v1/users/students/lookup` with
`{"studentIds": ["<uuid>"]}` and returns each matching student's
`studentId`, `firstName`, and `lastName`. Teachers and administrators may look
up requested students; students may only look up their own ID. The assignment
service uses this endpoint to add `studentName` to submission responses.

Quiz creation requires `courseId`, nonblank title (max
150), duration of at least one minute, nonnegative pass mark, and at least one
attempt. Update has the same title, duration, pass-mark, and attempt
constraints. A quiz question requires
nonblank text, question type, positive marks, order, and 4–5 valid options;
each option requires nonblank text, correctness flag, and order. Grading
requires marks from 0 to 100 and may include feedback. The service also
enforces course access and resource state. Assignment material attachment
requires a non-null `materialId`.

## Attendance (`attendance-service`)

Base path: `/api/v1/attendance`. All listed endpoints allow `ADMIN` and
`TEACHER`.

| Method | Path | Roles | Request / result |
|---|---|---|---|
| POST | `/sessions` | `ADMIN`, `TEACHER` | `CreateAttendanceSessionRequest` |
| GET | `/courses/{courseId}/sessions` | `ADMIN`, `TEACHER` | List course sessions |
| GET | `/sessions/{sessionId}` | `ADMIN`, `TEACHER` | Fetch session |
| PUT | `/sessions/{sessionId}/records` | `ADMIN`, `TEACHER` | `MarkAttendanceRequest` |
| POST | `/sessions/{sessionId}/complete` | `ADMIN`, `TEACHER` | Complete session |
| POST | `/sessions/{sessionId}/cancel` | `ADMIN`, `TEACHER` | Cancel session |
| GET | `/students/{studentId}/courses/{courseId}` | `ADMIN`, `TEACHER` | Student attendance summary |
| GET | `/courses/{courseId}/me` | `STUDENT` | Current student's attendance summary |

Session creation requires course ID, nonblank title (max 150), date, start
time, and end time. Marking requires a non-empty `records` list; each record
requires student ID and status, with optional remarks up to 500 characters.

## Materials (`material-service`)

Base path: `/api/v1/materials`.

| Method | Path | Roles | Request / result |
|---|---|---|---|
| POST | `/api/v1/materials` | `STUDENT`, `TEACHER`, `ADMIN`, `SUPER_ADMIN` | Multipart: required `title`, `courseId`, `file`; optional `description`, `purpose` (defaults to `LEARNING_MATERIAL`) |
| GET | `/api/v1/materials` | `STUDENT`, `TEACHER`, `ADMIN`, `SUPER_ADMIN` | Search/filter and pageable result |
| GET | `/api/v1/materials/{materialId}` | `STUDENT`, `TEACHER`, `ADMIN`, `SUPER_ADMIN` | Material detail |
| GET | `/api/v1/materials/{materialId}/download` | `STUDENT`, `TEACHER`, `ADMIN`, `SUPER_ADMIN` | Download URL |
| PUT | `/api/v1/materials/{materialId}` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | `UpdateMaterialRequest` (`title`, `description`) |
| POST | `/api/v1/materials/{materialId}/archive` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | Archive material |
| DELETE | `/api/v1/materials/{materialId}` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | Delete material |

List filters include `search`, `type`, `purpose`, `status`, `uploadedBy`,
`courseId`, `moduleId`, `lessonId`, `createdFrom`, and `createdTo`. Dates use
ISO date-time. Paging defaults to 20 rows, sorted by `createdAt` descending.
The upload route consumes `multipart/form-data`; it does not take a JSON DTO.

## Calendar (`calender-service`)

| Method | Path | Roles | Request / result |
|---|---|---|---|
| GET | `/api/v1/calendar/events?start=&end=&type=&courseId=&status=` | `STUDENT`, `TEACHER`, `ADMIN`, `SUPER_ADMIN` | Visible events |
| GET | `/api/v1/calendar/upcoming?limit=` | `STUDENT`, `TEACHER`, `ADMIN`, `SUPER_ADMIN` | Upcoming visible events |
| POST | `/api/v1/calendar/events` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | `CalendarEventRequest` |
| GET | `/api/v1/calendar/events/{eventId}` | `STUDENT`, `TEACHER`, `ADMIN`, `SUPER_ADMIN` | One visible event |
| PUT | `/api/v1/calendar/events/{eventId}` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | `CalendarEventRequest` |
| POST | `/api/v1/calendar/events/{eventId}/cancel` | `TEACHER`, `ADMIN`, `SUPER_ADMIN` | Cancel event |

`CalendarEventRequest` requires title (nonblank, max 200), event type,
start date-time, `allDay`, and visibility. Description is max 2000 and location
max 255. End must not precede start; timed events require an end. Course
visibility requires course scope, and role visibility requires a target role.
Role eligibility does not grant access to every course: course assignment,
enrollment, and event visibility are checked by the service. For date-range,
visibility, status, and response semantics, see
[Calendar Role-Based Query APIs](./calendar/CALENDAR_ROLE_BASED_QUERY_API.md)
and [Calendar Event Management API](./calendar/CALENDAR_EVENT_MANAGEMENT_API.md).

## Notifications (`notification-service`)

All routes use the authenticated JWT subject as the recipient; clients cannot
select another user's notification scope. The controller has no per-role
`@PreAuthorize` declarations, but requires an authenticated JWT to resolve the
recipient.

| Method | Path | Request / result |
|---|---|---|
| GET | `/api/v1/notifications?page=0&size=20&unreadOnly=false` | Notification page |
| GET | `/api/v1/notifications/unread-count` | Unread count |
| PATCH | `/api/v1/notifications/{notificationId}/read` | Mark one as read |
| PATCH | `/api/v1/notifications/read-all` | Mark all as read |
| DELETE | `/api/v1/notifications/{notificationId}` | Delete one |

`page` must be at least 0; `size` is 1–100 (default 20). No request DTO is
used for these endpoints.

## Exams (`exam-service`)

All `/api/v1/exams` routes below allow `TEACHER`, `ADMIN`, and `SUPER_ADMIN`.

| Method | Path | Request / result |
|---|---|---|
| POST | `/api/v1/exams` | `CreateExamRequest` |
| GET | `/api/v1/exams/{examId}` | Exam detail |
| GET | `/api/v1/exams/courses/{courseId}` | Course exams |
| PUT | `/api/v1/exams/{examId}` | `UpdateExamRequest` |
| POST | `/api/v1/exams/{examId}/cancel` | Cancel exam |
| POST | `/api/v1/exams/{examId}/schedule` | Schedule exam |
| POST | `/api/v1/exams/{examId}/start-marking` | Start marking |
| POST | `/api/v1/exams/{examId}/complete` | Complete exam |
| GET | `/api/v1/exams/{examId}/results` | Result sheet |
| GET | `/api/v1/exams/{examId}/summary` | Exam summary |
| PUT | `/api/v1/exams/{examId}/results` | `BatchMarksEntryRequest` |

Exam creation requires course ID, nonblank title (max 200), exam date,
start/end time, positive total marks, nonnegative pass marks, and exam type.
End time must follow start time and pass marks cannot exceed total marks.
Batch marks require a non-empty `results` list; each entry needs student ID
and attendance status, marks (if supplied) cannot be negative and may have at
most 2 decimal places, and remarks are max 1000. Creator, state, and audit
timestamps are server-managed.

## Validation and error expectations

Request DTO constraints are applied at controller parameters annotated with
`@Valid`; invalid JSON or constraint violations return HTTP 400. Authorization
failures return 401 or 403 according to authentication and role/scope checks.
The exact envelope for errors is owned by each service's exception handling;
do not assume every controller returns the same error DTO.

Response models are service DTOs rather than persistence entities: course
routes return `CourseResponse`, grade routes `GradeResponse` or
`GradeMonthlyFeeResponse`, enrollment routes `EntrollmentResponse` (or the
explicit boolean/course-ID/student-list result shown above), assignment and
grading routes `AssignmentResponse` and `SubmissionResponse`, quiz routes
`QuizResponse` and `TeacherQuestionResponse`, attendance routes
`AttendanceSessionResponse`, material routes `MaterialResponse`,
`MaterialDetailsResponse`, or `MaterialDownloadResponse`, calendar routes
`CalendarEventResponse`, notification routes `NotificationPageResponse` or
`NotificationResponse`, and exam routes `ExamResponse`,
`ExamResultSheetResponse`, `ExamSummaryResponse`, or `MarksEntryResponse`.

## CI coverage

The CI unit-test matrix includes course, enrollment, assignment/quiz,
attendance, material, calendar, notification, and exam services. The calendar
service must remain in the unit-test matrix as well as the build and
integration-test matrices.
