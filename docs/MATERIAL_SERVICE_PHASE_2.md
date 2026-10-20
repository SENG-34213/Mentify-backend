# Material Service Phase 2

Phase 2 adds a backward-compatible resource scope to `materials` without
changing existing public material response contracts or existing S3 keys.

## Resource model

Each material now has:

- `resource_type`: course, module, lesson, assignment, submission, exam,
  AI knowledge, or other.
- `resource_id`: the owning resource identifier.
- `course_id`: retained for course-level authorization and compatibility.
- `owner_id`: the application user who owns the file.
- `visibility`: course members, owner only, teacher only, admin only, or
  internal.

Existing rows are backfilled by `V6__add_generic_material_resource_scope.sql`:
course materials use `resource_id = course_id`, `owner_id = uploaded_by`, and
course-member visibility. Submission attachments use owner-only visibility.

The repository has historical V5 drift: some existing databases recorded V5 as
the legacy source-reference migration while the checked-in V5 file creates the
audit table. Do not edit V5 or delete the Flyway history. Before enabling
Flyway against such an existing database, verify the audit table is present and
run the normal controlled Flyway repair procedure to reconcile the recorded V5
checksum, then apply V6. This preserves existing records and is safer than
silently disabling validation.

## Internal APIs

With an end-user bearer token, downstream services can use:

```text
POST /api/v1/internal/materials/{materialId}/association
GET  /api/v1/internal/materials/{materialId}/association
```

The POST body is:

```json
{
  "resourceType": "ASSIGNMENT",
  "resourceId": "<assignment-id>",
  "courseId": "<course-id>",
  "ownerId": "<application-user-id>",
  "visibility": "COURSE_MEMBERS"
}
```

Admins may associate authorized material records. Teachers must own the
course. Students may associate only their own `SUBMISSION` with `OWNER_ONLY`
visibility and must be enrolled in the course. The endpoint never returns an
S3 key or credentials.

## Security behavior

Material download still creates presigned URLs only after authorization. Admins
retain management access; course teachers are checked against Course Service;
enrolled students can read course materials; and private submissions require
both enrollment and an owner match. A student cannot download another
student's submission by guessing its material ID.

## Migration status

No Course, Assignment, Exam, or AI file storage migration is performed in this
phase. Assignment still has legacy local-file support, Course has legacy
learning-material metadata, and AI consumes authorized material download URLs.
The next increment should migrate one flow at a time to create a material,
associate it with the owning resource, and retain the downstream service's
business authorization.
