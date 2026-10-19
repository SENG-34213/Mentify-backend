package com.mentify.material.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.material.client.CourseServiceClient;
import com.mentify.material.client.EnrollmentServiceClient;
import com.mentify.material.client.dto.CourseLookupResponse;
import com.mentify.material.client.dto.MaterialScopeLookupResponse;
import com.mentify.material.dto.request.MaterialAssociationRequest;
import com.mentify.material.dto.ValidatedMaterialFile;
import com.mentify.material.dto.response.S3DownloadResult;
import com.mentify.material.dto.response.S3UploadResult;
import com.mentify.material.entity.Material;
import com.mentify.material.enums.MaterialAuditAction;
import com.mentify.material.enums.MaterialPurpose;
import com.mentify.material.enums.MaterialResourceType;
import com.mentify.material.enums.MaterialStatus;
import com.mentify.material.enums.MaterialVisibility;
import com.mentify.material.repository.MaterialAuditEventRepository;
import com.mentify.material.repository.MaterialRepository;
import com.mentify.material.service.MaterialStorageService;
import com.mentify.payload.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MaterialServiceEndToEndTest {

    private static final String AUTH_HEADER = "Test token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private MaterialAuditEventRepository auditEventRepository;

    @Autowired
    private InMemoryMaterialStorageService storageService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private CourseServiceClient courseServiceClient;

    @MockBean
    private EnrollmentServiceClient enrollmentServiceClient;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        materialRepository.deleteAll();
        storageService.reset();
        reset(courseServiceClient, enrollmentServiceClient);
    }

    @Test
    void teacherUploadsCourseMaterialAndStudentDownloadsThroughPresignedUrl() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        mockCourse(courseId, teacherId);
        when(enrollmentServiceClient.isStudentEnrolledInCourse(studentId, courseId, AUTH_HEADER)).thenReturn(true);

        UUID materialId = uploadMaterial(
                teacherId,
                "TEACHER",
                courseId,
                MaterialPurpose.LEARNING_MATERIAL,
                "Algebra intro",
                pdf("algebra.pdf")
        );

        Material persisted = materialRepository.findById(materialId).orElseThrow();
        assertThat(persisted.getCourseId()).isEqualTo(courseId);
        assertThat(persisted.getPurpose()).isEqualTo(MaterialPurpose.LEARNING_MATERIAL);
        assertThat(storageService.exists(persisted.getStorageKey())).isTrue();

        String downloadResponse = mockMvc.perform(get("/api/v1/materials/{materialId}/download", materialId)
                        .header("Authorization", AUTH_HEADER)
                        .with(jwtUser(studentId, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(materialId.toString()))
                .andExpect(jsonPath("$.data.downloadUrl").value(org.hamcrest.Matchers.startsWith("https://downloads.test/")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(downloadResponse).doesNotContain("AWS_SECRET_ACCESS_KEY", "material-service-test-bucket", "storageKey");
        assertThat(auditEventRepository.findAll()).extracting(event -> event.getAction())
                .contains(MaterialAuditAction.UPLOAD, MaterialAuditAction.DOWNLOAD_ACCESS_GENERATED);
    }

    @Test
    void courseModuleAndLessonScopeFiltersReturnOnlyScopedMaterials() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        mockCourse(courseId, teacherId);
        when(enrollmentServiceClient.getActiveCourseIdsForStudent(studentId, AUTH_HEADER)).thenReturn(
                ApiResponse.success(HttpStatus.OK.value(), "ok", List.of(courseId))
        );

        UUID scopedMaterialId = uploadMaterial(teacherId, "TEACHER", courseId, MaterialPurpose.LEARNING_MATERIAL, "Vectors", pdf("vectors.pdf"));
        UUID otherMaterialId = uploadMaterial(teacherId, "TEACHER", courseId, MaterialPurpose.LEARNING_MATERIAL, "Geometry", pdf("geometry.pdf"));
        when(courseServiceClient.getModuleMaterialScope(moduleId, AUTH_HEADER))
                .thenReturn(scope(courseId, List.of(scopedMaterialId)));
        when(courseServiceClient.getLessonMaterialScope(lessonId, AUTH_HEADER))
                .thenReturn(scope(courseId, List.of(scopedMaterialId)));

        mockMvc.perform(get("/api/v1/materials")
                        .param("moduleId", moduleId.toString())
                        .header("Authorization", AUTH_HEADER)
                        .with(jwtUser(studentId, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(scopedMaterialId.toString()));

        mockMvc.perform(get("/api/v1/materials")
                        .param("lessonId", lessonId.toString())
                        .header("Authorization", AUTH_HEADER)
                        .with(jwtUser(studentId, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(scopedMaterialId.toString()));

        assertThat(otherMaterialId).isNotEqualTo(scopedMaterialId);
    }

    @Test
    void assignmentAttachmentAndSubmissionFilePurposesWorkAcrossTeacherAndStudentRoles() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        mockCourse(courseId, teacherId);
        when(enrollmentServiceClient.isStudentEnrolledInCourse(studentId, courseId, AUTH_HEADER)).thenReturn(true);

        UUID assignmentAttachmentId = uploadMaterial(
                teacherId,
                "TEACHER",
                courseId,
                MaterialPurpose.ASSIGNMENT_ATTACHMENT,
                "Assignment brief",
                pdf("brief.pdf")
        );
        mockMvc.perform(get("/api/v1/materials/{materialId}/download", assignmentAttachmentId)
                        .header("Authorization", AUTH_HEADER)
                        .with(jwtUser(studentId, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.purpose").value("ASSIGNMENT_ATTACHMENT"));

        UUID submissionFileId = uploadMaterial(
                studentId,
                "STUDENT",
                courseId,
                MaterialPurpose.SUBMISSION_ATTACHMENT,
                "Student answer",
                pdf("answer.pdf")
        );
        mockMvc.perform(get("/api/v1/materials/{materialId}/download", submissionFileId)
                        .header("Authorization", AUTH_HEADER)
                        .with(jwtUser(teacherId, "TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.purpose").value("SUBMISSION_ATTACHMENT"));
    }

    @Test
    void privateSubmissionIsNotVisibleToAnotherEnrolledStudentAndCanBeAssociated() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID ownerStudentId = UUID.randomUUID();
        UUID otherStudentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        mockCourse(courseId, teacherId);
        when(enrollmentServiceClient.isStudentEnrolledInCourse(ownerStudentId, courseId, AUTH_HEADER)).thenReturn(true);
        when(enrollmentServiceClient.isStudentEnrolledInCourse(otherStudentId, courseId, AUTH_HEADER)).thenReturn(true);

        UUID materialId = uploadMaterial(
                ownerStudentId,
                "STUDENT",
                courseId,
                MaterialPurpose.SUBMISSION_ATTACHMENT,
                "Private submission",
                pdf("submission.pdf")
        );

        mockMvc.perform(get("/api/v1/materials/{materialId}/download", materialId)
                        .header("Authorization", AUTH_HEADER)
                        .with(jwtUser(otherStudentId, "STUDENT")))
                .andExpect(status().isForbidden());

        MaterialAssociationRequest association = new MaterialAssociationRequest(
                MaterialResourceType.SUBMISSION,
                UUID.randomUUID(),
                courseId,
                ownerStudentId,
                MaterialVisibility.OWNER_ONLY
        );
        mockMvc.perform(post("/api/v1/internal/materials/{materialId}/association", materialId)
                        .header("Authorization", AUTH_HEADER)
                        .with(jwtUser(UUID.randomUUID(), "ADMIN"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(association)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resourceType").value("SUBMISSION"))
                .andExpect(jsonPath("$.data.visibility").value("OWNER_ONLY"));
    }

    @Test
    void aiServiceCanUseAuthorizedMaterialDownloadContractWithoutStorageDetails() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        mockCourse(courseId, teacherId);
        UUID materialId = uploadMaterial(teacherId, "TEACHER", courseId, MaterialPurpose.AI_SOURCE, "AI source notes", pdf("ai-notes.pdf"));

        mockMvc.perform(get("/api/v1/materials/{materialId}", materialId)
                        .header("Authorization", AUTH_HEADER)
                        .with(jwtUser(teacherId, "TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(materialId.toString()))
                .andExpect(jsonPath("$.data.purpose").value("AI_SOURCE"))
                .andExpect(jsonPath("$.data.storageKey").doesNotExist())
                .andExpect(jsonPath("$.data.downloadUrl").doesNotExist());

        String response = mockMvc.perform(get("/api/v1/materials/{materialId}/download", materialId)
                        .header("Authorization", AUTH_HEADER)
                        .with(jwtUser(teacherId, "TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.downloadUrl").exists())
                .andExpect(jsonPath("$.data.storageKey").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).doesNotContain("AWS_ACCESS_KEY_ID", "AWS_SECRET_ACCESS_KEY", "material-service-test-bucket");
    }

    @Test
    void searchFilterPaginationArchiveAndDeleteBehaveSafely() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        mockCourse(courseId, teacherId);

        UUID algebraId = uploadMaterial(teacherId, "TEACHER", courseId, MaterialPurpose.LEARNING_MATERIAL, "Algebra workbook", pdf("algebra.pdf"));
        uploadMaterial(teacherId, "TEACHER", courseId, MaterialPurpose.ASSIGNMENT_ATTACHMENT, "Geometry brief", pdf("geometry.pdf"));
        when(courseServiceClient.getMyCourses(AUTH_HEADER)).thenReturn(ApiResponse.success(
                HttpStatus.OK.value(),
                "ok",
                List.of(new CourseLookupResponse(courseId, teacherId))
        ));

        mockMvc.perform(get("/api/v1/materials")
                        .param("search", "algebra")
                        .param("purpose", "LEARNING_MATERIAL")
                        .param("page", "0")
                        .param("size", "1")
                        .header("Authorization", AUTH_HEADER)
                        .with(jwtUser(teacherId, "TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(algebraId.toString()))
                .andExpect(jsonPath("$.data.size").value(1));

        mockMvc.perform(post("/api/v1/materials/{materialId}/archive", algebraId)
                        .header("Authorization", AUTH_HEADER)
                        .with(jwtUser(adminId, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        Material archived = materialRepository.findById(algebraId).orElseThrow();
        assertThat(archived.getStatus()).isEqualTo(MaterialStatus.ARCHIVED);
        assertThat(storageService.exists(archived.getStorageKey())).isTrue();

        mockMvc.perform(delete("/api/v1/materials/{materialId}", algebraId)
                        .header("Authorization", AUTH_HEADER)
                        .with(jwtUser(adminId, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELETED"));

        Material deleted = materialRepository.findById(algebraId).orElseThrow();
        assertThat(deleted.isActive()).isFalse();
        assertThat(storageService.exists(deleted.getStorageKey())).isTrue();

        mockMvc.perform(get("/api/v1/materials/{materialId}", algebraId)
                        .header("Authorization", AUTH_HEADER)
                        .with(jwtUser(adminId, "ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidUnauthorizedAndStorageFailureScenariosAreBlockedSafely() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID otherStudentId = UUID.randomUUID();
        mockCourse(courseId, teacherId);
        when(enrollmentServiceClient.isStudentEnrolledInCourse(studentId, courseId, AUTH_HEADER)).thenReturn(true);
        when(enrollmentServiceClient.isStudentEnrolledInCourse(otherStudentId, courseId, AUTH_HEADER)).thenReturn(false);

        mockMvc.perform(multipart("/api/v1/materials")
                        .file(pdf("unauthorized.pdf"))
                        .param("title", "Unauthorized")
                        .param("purpose", "LEARNING_MATERIAL")
                        .param("courseId", courseId.toString())
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(multipart("/api/v1/materials")
                        .file(pdf("blocked.pdf"))
                        .param("title", "Blocked")
                        .param("purpose", "LEARNING_MATERIAL")
                        .param("courseId", courseId.toString())
                        .header("Authorization", AUTH_HEADER)
                        .with(jwtUser(studentId, "STUDENT")))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/api/v1/materials")
                        .file(new MockMultipartFile("file", "malware.exe", "application/octet-stream", "bad".getBytes()))
                        .param("title", "Bad file")
                        .param("purpose", "LEARNING_MATERIAL")
                        .param("courseId", courseId.toString())
                        .header("Authorization", AUTH_HEADER)
                        .with(jwtUser(teacherId, "TEACHER")))
                .andExpect(status().isBadRequest());

        UUID materialId = uploadMaterial(teacherId, "TEACHER", courseId, MaterialPurpose.LEARNING_MATERIAL, "Private notes", pdf("private.pdf"));
        mockMvc.perform(get("/api/v1/materials/{materialId}/download", materialId)
                        .header("Authorization", AUTH_HEADER)
                        .with(jwtUser(otherStudentId, "STUDENT")))
                .andExpect(status().isForbidden());

        storageService.failUploads = true;
        mockMvc.perform(multipart("/api/v1/materials")
                        .file(pdf("s3-fail.pdf"))
                        .param("title", "S3 fail")
                        .param("purpose", "LEARNING_MATERIAL")
                        .param("courseId", courseId.toString())
                        .header("Authorization", AUTH_HEADER)
                        .with(jwtUser(teacherId, "TEACHER")))
                .andExpect(status().isServiceUnavailable());
        assertThat(materialRepository.findAll()).extracting(Material::getTitle).doesNotContain("S3 fail");
    }

    private UUID uploadMaterial(
            UUID userId,
            String role,
            UUID courseId,
            MaterialPurpose purpose,
            String title,
            MockMultipartFile file
    ) throws Exception {
        String response = mockMvc.perform(multipart("/api/v1/materials")
                        .file(file)
                        .param("title", title)
                        .param("description", "E2E test material")
                        .param("purpose", purpose.name())
                        .param("courseId", courseId.toString())
                        .header("Authorization", AUTH_HEADER)
                        .with(jwtUser(userId, role)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.title").value(title))
                .andExpect(jsonPath("$.data.courseId").value(courseId.toString()))
                .andExpect(jsonPath("$.data.purpose").value(purpose.name()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        assertThat(response).doesNotContain("AWS_SECRET_ACCESS_KEY", "material-service-test-bucket");
        return UUID.fromString(root.path("data").path("id").asText());
    }

    private void mockCourse(UUID courseId, UUID teacherId) {
        when(courseServiceClient.getCourseById(courseId, AUTH_HEADER)).thenReturn(
                ApiResponse.success(HttpStatus.OK.value(), "ok", new CourseLookupResponse(courseId, teacherId))
        );
    }

    private ApiResponse<MaterialScopeLookupResponse> scope(UUID courseId, List<UUID> materialIds) {
        return ApiResponse.success(HttpStatus.OK.value(), "ok", new MaterialScopeLookupResponse(courseId, materialIds));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jwtUser(UUID userId, String role) {
        return jwt()
                .jwt(jwt -> jwt.subject(userId.toString()).claim("realm_access", Map.of("roles", List.of(role))))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private MockMultipartFile pdf(String filename) {
        return new MockMultipartFile("file", filename, "application/pdf", "%PDF-1.7 e2e data".getBytes());
    }

    @TestConfiguration
    static class TestStorageConfiguration {

        @Bean
        @Primary
        InMemoryMaterialStorageService inMemoryMaterialStorageService() {
            return new InMemoryMaterialStorageService();
        }
    }

    static class InMemoryMaterialStorageService implements MaterialStorageService {

        private final Map<String, byte[]> objects = new HashMap<>();
        private boolean failUploads;

        @Override
        public S3UploadResult upload(MultipartFile file, ValidatedMaterialFile validatedFile) {
            if (failUploads) {
                throw new com.mentify.material.exception.MaterialStorageException("Material file upload failed");
            }
            String key = "materials/e2e-" + UUID.randomUUID() + "." + validatedFile.extension();
            try {
                objects.put(key, file.getBytes());
            } catch (IOException ex) {
                throw new com.mentify.material.exception.MaterialStorageException("Material file upload failed", ex);
            }
            return new S3UploadResult(key);
        }

        @Override
        public S3DownloadResult createPresignedDownload(String storageKey, String originalFilename, String contentType) {
            if (!exists(storageKey)) {
                throw new com.mentify.material.exception.MaterialStorageException("Material file is unavailable");
            }
            return new S3DownloadResult(
                    "https://downloads.test/" + storageKey + "?X-Amz-Signature=fake",
                    Instant.now().plusSeconds(300)
            );
        }

        @Override
        public boolean exists(String storageKey) {
            return objects.containsKey(storageKey);
        }

        @Override
        public List<String> listStorageKeys() {
            return List.copyOf(objects.keySet());
        }

        @Override
        public void delete(String storageKey) {
            objects.remove(storageKey);
        }

        void reset() {
            objects.clear();
            failUploads = false;
        }
    }
}
