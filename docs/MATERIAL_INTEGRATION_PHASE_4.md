# Material Integration — Phase 4

## Exam attachments

Exam Service now stores exam attachment metadata and the Material Service ID in `exam_attachments`. S3 operations remain exclusively in Material Service.

Endpoints:

- `POST /api/v1/exams/{examId}/attachments?studentVisible=false` — teacher/admin upload.
- `GET /api/v1/exams/{examId}/attachments` — teacher/admin access; enrolled students only see released attachments.
- `GET /api/v1/exams/{examId}/attachments/{attachmentId}/download` — returns a protected presigned URL.
- `DELETE /api/v1/exams/{examId}/attachments/{attachmentId}` — soft-deactivates the domain reference.

Uploads are associated as `EXAM` materials with `EXAM_ATTACHMENT` purpose. Unreleased attachments use `TEACHER_ONLY`; released attachments use `COURSE_MEMBERS`. Students must be enrolled and the exam must be `SCHEDULED`. Draft, marking, completed, and cancelled exams are not student-readable. Soft deletion intentionally leaves S3 cleanup to Material Service/reconciliation so shared or still-referenced objects are not deleted accidentally.

## AI document access and processing

AI continues to retrieve material metadata and presigned URLs through Material Service using the caller's bearer token. Material Service remains responsible for course enrollment, teacher ownership, private-submission, and visibility checks; AI never receives AWS credentials or directly accesses the bucket. Existing PDF, DOCX, and TXT extraction remains in place, and PPTX recognition/extraction is now supported through Apache POI already present in `ai-service`.

Material summarization and material-backed quiz generation now return additive source metadata when a Material ID is supplied. Prompt guardrails continue to treat document text as untrusted content. The current repository has no vector database or embedding service, so Phase 4 does not pretend to provide persistent RAG: retrieval is authorized Material Service retrieval followed by bounded extraction and provider prompting. Persistent chunking, embeddings, vector storage, and citation-level answer grounding remain a later phase.

## Migration

`V5__create_exam_attachments.sql` creates the attachment reference table and preserves existing exam records. The Exam test profile explicitly enables Flyway because the local root `.env` currently contains `FLYWAY_ENABLED=false`; production configuration remains controlled by Config Server.
