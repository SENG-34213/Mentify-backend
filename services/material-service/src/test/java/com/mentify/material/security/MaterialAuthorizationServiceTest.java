package com.mentify.material.security;

import com.mentify.material.client.CourseServiceClient;
import com.mentify.material.client.EnrollmentServiceClient;
import com.mentify.material.client.dto.CourseLookupResponse;
import com.mentify.material.entity.Material;
import com.mentify.material.enums.MaterialPurpose;
import com.mentify.material.enums.MaterialStatus;
import com.mentify.material.enums.MaterialType;
import com.mentify.material.exception.MaterialValidationException;
import com.mentify.payload.response.ApiResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialAuthorizationServiceTest {

    private static final String AUTH_HEADER = "Bearer token";

    @Mock
    private CourseServiceClient courseServiceClient;

    @Mock
    private EnrollmentServiceClient enrollmentServiceClient;

    private MaterialAuthorizationServiceImpl authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new MaterialAuthorizationServiceImpl(
                new CurrentMaterialUserService(),
                courseServiceClient,
                enrollmentServiceClient
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminAccessWorksForCourseUpload() {
        UUID adminId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        authenticate(adminId, "ADMIN");
        when(courseServiceClient.getCourseById(courseId, AUTH_HEADER))
                .thenReturn(courseResponse(courseId, UUID.randomUUID()));

        assertThat(authorizationService.requireUploadCourseAccess(courseId, MaterialPurpose.LEARNING_MATERIAL, AUTH_HEADER)).isEqualTo(courseId);
    }

    @Test
    void superAdminCanManageAnyMaterial() {
        authenticate(UUID.randomUUID(), "SUPER_ADMIN");
        Material material = material(UUID.randomUUID(), UUID.randomUUID());

        authorizationService.assertCanManage(material, AUTH_HEADER);

        verify(courseServiceClient, never()).getCourseById(material.getCourseId(), AUTH_HEADER);
    }

    @Test
    void authorizedTeacherAccessWorksForAssignedCourse() {
        UUID teacherId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        authenticate(teacherId, "TEACHER");
        when(courseServiceClient.getCourseById(courseId, AUTH_HEADER))
                .thenReturn(courseResponse(courseId, teacherId));

        assertThat(authorizationService.requireUploadCourseAccess(courseId, MaterialPurpose.LEARNING_MATERIAL, AUTH_HEADER)).isEqualTo(courseId);
    }

    @Test
    void unauthorizedTeacherAccessIsBlocked() {
        UUID teacherId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        authenticate(teacherId, "TEACHER");
        when(courseServiceClient.getCourseById(courseId, AUTH_HEADER))
                .thenReturn(courseResponse(courseId, UUID.randomUUID()));

        assertThatThrownBy(() -> authorizationService.requireUploadCourseAccess(courseId, MaterialPurpose.LEARNING_MATERIAL, AUTH_HEADER))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("User is not authorized to upload material for this course");
    }

    @Test
    void enrolledStudentCanAccessAllowedMaterial() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        authenticate(studentId, "STUDENT");
        when(enrollmentServiceClient.isStudentEnrolledInCourse(studentId, courseId, AUTH_HEADER)).thenReturn(true);

        authorizationService.assertCanDownload(material(courseId, UUID.randomUUID()), AUTH_HEADER);
    }

    @Test
    void nonEnrolledStudentIsBlocked() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        authenticate(studentId, "STUDENT");
        when(enrollmentServiceClient.isStudentEnrolledInCourse(studentId, courseId, AUTH_HEADER)).thenReturn(false);

        assertThatThrownBy(() -> authorizationService.assertCanView(material(courseId, UUID.randomUUID()), AUTH_HEADER))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("User is not authorized to view this material");
    }

    @Test
    void materialWithoutCourseScopeCannotBeGuessedByStudent() {
        authenticate(UUID.randomUUID(), "STUDENT");

        assertThatThrownBy(() -> authorizationService.assertCanDownload(material(null, UUID.randomUUID()), AUTH_HEADER))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("User is not authorized to download this material");
    }

    @Test
    void uploadRequiresCourseScope() {
        authenticate(UUID.randomUUID(), "TEACHER");

        assertThatThrownBy(() -> authorizationService.requireUploadCourseAccess(null, MaterialPurpose.LEARNING_MATERIAL, AUTH_HEADER))
                .isInstanceOf(MaterialValidationException.class)
                .hasMessage("Course id is required for material upload");
    }

    @Test
    void jwtDerivedIdentityIsEnforcedForTeacherAccess() {
        UUID tokenTeacherId = UUID.randomUUID();
        UUID frontendClaimedTeacherId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        authenticate(tokenTeacherId, "TEACHER");
        when(courseServiceClient.getCourseById(courseId, AUTH_HEADER))
                .thenReturn(courseResponse(courseId, frontendClaimedTeacherId));

        assertThatThrownBy(() -> authorizationService.requireUploadCourseAccess(courseId, MaterialPurpose.LEARNING_MATERIAL, AUTH_HEADER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void enrolledStudentCanUploadSubmissionAttachmentOnly() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        authenticate(studentId, "STUDENT");
        when(courseServiceClient.getCourseById(courseId, AUTH_HEADER))
                .thenReturn(courseResponse(courseId, UUID.randomUUID()));
        when(enrollmentServiceClient.isStudentEnrolledInCourse(studentId, courseId, AUTH_HEADER)).thenReturn(true);

        assertThat(authorizationService.requireUploadCourseAccess(courseId, MaterialPurpose.SUBMISSION_ATTACHMENT, AUTH_HEADER))
                .isEqualTo(courseId);
        assertThatThrownBy(() -> authorizationService.requireUploadCourseAccess(courseId, MaterialPurpose.LEARNING_MATERIAL, AUTH_HEADER))
                .isInstanceOf(AccessDeniedException.class);
    }

    private ApiResponse<CourseLookupResponse> courseResponse(UUID courseId, UUID assignedTeacherId) {
        return ApiResponse.success(
                HttpStatus.OK.value(),
                "ok",
                new CourseLookupResponse(courseId, assignedTeacherId)
        );
    }

    private Material material(UUID courseId, UUID uploadedBy) {
        Material material = Material.builder()
                .title("Algebra intro")
                .description("Week 1 handout")
                .originalFilename("intro.pdf")
                .storageKey("materials/object.pdf")
                .contentType("application/pdf")
                .fileSize(12L)
                .type(MaterialType.PDF)
                .purpose(MaterialPurpose.LEARNING_MATERIAL)
                .status(MaterialStatus.AVAILABLE)
                .uploadedBy(uploadedBy)
                .courseId(courseId)
                .build();
        material.setId(UUID.randomUUID());
        material.setActive(true);
        return material;
    }

    private void authenticate(UUID userId, String role) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(userId.toString())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + role)))
        );
    }
}
