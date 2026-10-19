# Material Integration — Phase 3

Course Service and Assignment Service now delegate new file uploads to Material Service. They retain only domain references (`materialId`) and legacy local-file fields needed to read old records. Neither service creates an S3 client.

## Endpoints

- `POST /api/v1/course/{courseId}/modules/{moduleId}/learning-materials/upload` — teacher/admin multipart upload; optional `lessonId`.
- `POST /api/v1/assignments/{assignmentId}/attachments/upload` — teacher/admin multipart upload.
- `POST /api/v1/submissions/{submissionId}/files/upload` — student multipart upload for their own draft submission.

The existing JSON attach, list, download, and delete endpoints remain available. Material-backed downloads request a protected presigned URL from Material Service. Submission visibility remains owner-only for students; teachers can read submissions for assignments they manage.

## Failure handling

Each new upload associates the returned material before saving the local domain reference. If association or persistence fails, the integration makes a best-effort delete request to Material Service. A later reconciliation job should retry any cleanup that fails while a downstream service is unavailable.

## Database and legacy files

No Course or Assignment migration is required: both services already have nullable `material_id` columns. Existing local files are not deleted or rewritten. A safe migration should enumerate legacy rows, upload each file through Material Service, verify checksum and download, update the reference in a transaction, and only then archive the local file. The migration must be restartable and produce an audit/reconciliation report; it is intentionally not run automatically by this phase.

## Service dependencies

Course and Assignment use their existing Eureka/OpenFeign clients and forward the caller's `Authorization` header. Material Service remains the S3 owner. Enrollment remains the source of student course access checks; no new Course↔Assignment dependency was introduced.
