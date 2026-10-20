# Material Service Phase 1

## Authorization fix

Material upload authorizes the course through `CourseServiceClient` and then
compares the authenticated user with `assignedTeacherId`. Course Service
resolves the application user from the `local_user_id` JWT claim first, while
Material Service was resolving the Keycloak `sub` claim first. Those values are
different in the normal Mentify realm, so a valid teacher could fail the course
ownership check. Material Service now uses the same claim precedence as Course
Service and still falls back to `sub` for tokens that do not contain the local
claim.

The message `Material resource authorization could not be verified` is also
returned when the downstream course lookup is genuinely HTTP 404. In that case
the `courseId` supplied to upload is not present in Course Service's database
or the services are using different environments. Check the course lookup
endpoint with the same bearer token before retrying upload.

## Local start and S3 verification

The repository root `.env` is loaded by `common-lib`'s
`DotenvEnvironmentPostProcessor`; it searches the working directory and parent
directories. The existing S3 settings are:

```text
AWS_PROFILE=mentify-dev
AWS_REGION=ap-south-1
MATERIAL_S3_BUCKET=mentify-materials-dev-unique123-193908537177-ap-south-1-an
MATERIAL_DOWNLOAD_URL_EXPIRATION=PT5M
```

Start Config Server, Eureka, Course Service, and Material Service from the
repository root. For Maven, use:

```bash
sh cloud/config-server/mvnw -f pom.xml -pl cloud/config-server spring-boot:run
sh cloud/service-registry/mvnw -f pom.xml spring-boot:run
sh cloud/config-server/mvnw -f pom.xml -pl services/course-service spring-boot:run
sh cloud/config-server/mvnw -f pom.xml -pl services/material-service spring-boot:run
```

IntelliJ must use the repository root as the working directory so the shared
`.env` is found. `S3Config` uses the configured profile with the AWS SDK
`DefaultCredentialsProvider`; production can omit `AWS_PROFILE` and use the
normal role or temporary-credential chain.

## Postman smoke test

1. Obtain a bearer token for an admin or the teacher assigned to the course.
2. Confirm the course exists with `GET http://localhost:8082/api/v1/course/{courseId}/lookup`.
3. Upload with `POST http://localhost:8091/api/v1/materials`, `Authorization:
   Bearer <token>`, and multipart fields `title`, `description`, `purpose`,
   `courseId`, and `file`.
4. Use the returned material ID with
   `GET /api/v1/materials/{materialId}/download` and verify a presigned URL is
   returned. The bucket remains private; do not make the object public.
5. Verify the object and metadata independently:

```bash
aws s3api head-object --bucket "$MATERIAL_S3_BUCKET" \
  --key '<storage-key-from-service-log-or-database>' \
  --profile mentify-dev --region ap-south-1
```

The repository tests use an in-memory storage double. Successful unit tests do
not by themselves prove AWS connectivity; the CLI check or an actual local
upload is required for that claim.

## Phase 2 plan: centralized material management

- Keep Material Service as the owner of S3 keys, private objects, presigned
  URLs, metadata, audit events, and cleanup.
- Migrate Assignment Service's `LocalFileStorageService` attachments and
  submission files to Material Service references, retaining assignment and
  submission authorization in Assignment Service.
- Replace any future Course Service file uploads with material references;
  Course Service should retain only course/module/lesson ownership and scope.
- Update Exam Service file features, if introduced, to use the same reference
  contract rather than a second bucket or filesystem.
- Keep AI Service as a consumer: request an authorized material download URL,
  fetch the private object, and extract content without receiving S3
  credentials or storage keys.
- Introduce the migration incrementally with dual-read compatibility, then
  backfill references and remove local-file writes only after all consumers are
  verified. No cross-service migration is performed in Phase 1.
