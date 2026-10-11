package com.mentify.assignment.service.impl;

import com.mentify.assignment.client.MaterialServiceClient;
import com.mentify.assignment.client.dto.MaterialDownloadResponse;
import com.mentify.assignment.client.dto.MaterialLookupResponse;
import com.mentify.assignment.dto.request.AttachAssignmentMaterialRequest;
import com.mentify.assignment.dto.response.AssignmentAttachmentDownloadResponse;
import com.mentify.assignment.dto.response.AssignmentAttachmentResponse;
import com.mentify.assignment.entity.Assignment;
import com.mentify.assignment.entity.AssignmentAttachment;
import com.mentify.assignment.enums.AssignmentStatus;
import com.mentify.assignment.repository.AssignmentAttachmentRepository;
import com.mentify.assignment.repository.AssignmentRepository;
import com.mentify.assignment.storage.FileStorageService;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.client.CourseServiceClient;
import com.mentify.quiz.client.EnrollmentServiceClient;
import com.mentify.quiz.client.dto.CourseLookupResponse;
import com.mentify.quiz.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentAttachmentServiceImplTest {

    private static final String AUTH_HEADER = "Bearer token";

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AssignmentAttachmentRepository attachmentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private MaterialServiceClient materialServiceClient;

    @Mock
    private CourseServiceClient courseServiceClient;

    @Mock
    private EnrollmentServiceClient enrollmentServiceClient;

    @Mock
    private CurrentUserService currentUserService;

    private AssignmentAttachmentServiceImpl attachmentService;

    @BeforeEach
    void setUp() {
        attachmentService = new AssignmentAttachmentServiceImpl(
                assignmentRepository,
                attachmentRepository,
                fileStorageService,
                materialServiceClient,
                courseServiceClient,
                enrollmentServiceClient,
                currentUserService
        );
    }

    @Test
    void attachMaterial_whenMaterialIsValid_persistsMaterialReference() {
        UUID assignmentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        Assignment assignment = assignment(assignmentId, courseId, teacherId, AssignmentStatus.DRAFT);
        MaterialLookupResponse material = material(materialId, courseId);

        when(assignmentRepository.findByIdAndIsActiveTrue(assignmentId)).thenReturn(Optional.of(assignment));
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(teacherId);
        when(courseServiceClient.getCourseById(courseId, AUTH_HEADER)).thenReturn(courseResponse(courseId, teacherId));
        when(materialServiceClient.getMaterial(materialId, AUTH_HEADER))
                .thenReturn(ApiResponse.success(HttpStatus.OK.value(), "ok", material));
        when(attachmentRepository.save(any(AssignmentAttachment.class))).thenAnswer(invocation -> {
            AssignmentAttachment attachment = invocation.getArgument(0);
            attachment.setId(UUID.randomUUID());
            return attachment;
        });

        ApiResponse<AssignmentAttachmentResponse> response = attachmentService.attachMaterial(
                assignmentId,
                AttachAssignmentMaterialRequest.builder().materialId(materialId).build(),
                AUTH_HEADER
        );

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getData().getMaterialId()).isEqualTo(materialId);
        assertThat(response.getData().isMaterialBacked()).isTrue();

        ArgumentCaptor<AssignmentAttachment> captor = ArgumentCaptor.forClass(AssignmentAttachment.class);
        verify(attachmentRepository).save(captor.capture());
        assertThat(captor.getValue().getMaterialId()).isEqualTo(materialId);
        assertThat(captor.getValue().getStoredFilename()).isEqualTo("material:" + materialId);
        assertThat(captor.getValue().getOriginalFilename()).isEqualTo("brief.pdf");
        verify(fileStorageService, never()).store(any());
    }

    @Test
    void attachMaterial_whenMaterialIdIsInvalid_throwsNotFound() {
        UUID assignmentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        Assignment assignment = assignment(assignmentId, courseId, teacherId, AssignmentStatus.DRAFT);

        when(assignmentRepository.findByIdAndIsActiveTrue(assignmentId)).thenReturn(Optional.of(assignment));
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(teacherId);
        when(courseServiceClient.getCourseById(courseId, AUTH_HEADER)).thenReturn(courseResponse(courseId, teacherId));
        when(materialServiceClient.getMaterial(materialId, AUTH_HEADER))
                .thenReturn(ApiResponse.success(HttpStatus.OK.value(), "ok", null));

        assertThatThrownBy(() -> attachmentService.attachMaterial(
                assignmentId,
                AttachAssignmentMaterialRequest.builder().materialId(materialId).build(),
                AUTH_HEADER
        )).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Material not found with id: '" + materialId + "'");
    }

    @Test
    void attachMaterial_whenTeacherDoesNotOwnAssignment_throwsAccessDenied() {
        UUID assignmentId = UUID.randomUUID();
        UUID assignmentTeacherId = UUID.randomUUID();
        UUID currentTeacherId = UUID.randomUUID();
        Assignment assignment = assignment(assignmentId, UUID.randomUUID(), assignmentTeacherId, AssignmentStatus.DRAFT);

        when(assignmentRepository.findByIdAndIsActiveTrue(assignmentId)).thenReturn(Optional.of(assignment));
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(currentTeacherId);

        assertThatThrownBy(() -> attachmentService.attachMaterial(
                assignmentId,
                AttachAssignmentMaterialRequest.builder().materialId(UUID.randomUUID()).build(),
                AUTH_HEADER
        )).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void listAttachments_whenStudentIsEnrolled_returnsAttachments() {
        UUID assignmentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Assignment assignment = assignment(assignmentId, courseId, UUID.randomUUID(), AssignmentStatus.PUBLISHED);
        AssignmentAttachment attachment = materialAttachment(assignment, UUID.randomUUID());

        when(assignmentRepository.findByIdAndIsActiveTrue(assignmentId)).thenReturn(Optional.of(assignment));
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(false);
        when(currentUserService.hasAnyRole("STUDENT")).thenReturn(true);
        when(enrollmentServiceClient.isStudentEnrolledInCourse(studentId, courseId, AUTH_HEADER)).thenReturn(true);
        when(attachmentRepository.findByAssignmentIdAndIsActiveTrueOrderByCreatedAtAsc(assignmentId))
                .thenReturn(List.of(attachment));

        ApiResponse<List<AssignmentAttachmentResponse>> response =
                attachmentService.listAttachments(assignmentId, AUTH_HEADER);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getMaterialId()).isEqualTo(attachment.getMaterialId());
    }

    @Test
    void listAttachments_whenStudentIsNotEnrolled_throwsAccessDenied() {
        UUID assignmentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Assignment assignment = assignment(assignmentId, courseId, UUID.randomUUID(), AssignmentStatus.PUBLISHED);

        when(assignmentRepository.findByIdAndIsActiveTrue(assignmentId)).thenReturn(Optional.of(assignment));
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(false);
        when(currentUserService.hasAnyRole("STUDENT")).thenReturn(true);
        when(enrollmentServiceClient.isStudentEnrolledInCourse(studentId, courseId, AUTH_HEADER)).thenReturn(false);

        assertThatThrownBy(() -> attachmentService.listAttachments(assignmentId, AUTH_HEADER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void downloadAttachment_whenMaterialBacked_returnsMaterialDownloadUrl() {
        UUID assignmentId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        Assignment assignment = assignment(assignmentId, courseId, teacherId, AssignmentStatus.DRAFT);
        AssignmentAttachment attachment = materialAttachment(assignment, materialId);
        attachment.setId(attachmentId);
        Instant expiresAt = Instant.now().plusSeconds(300);

        when(assignmentRepository.findByIdAndIsActiveTrue(assignmentId)).thenReturn(Optional.of(assignment));
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.getCurrentUserId()).thenReturn(teacherId);
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(true);
        when(attachmentRepository.findByIdAndAssignmentIdAndIsActiveTrue(attachmentId, assignmentId))
                .thenReturn(Optional.of(attachment));
        when(materialServiceClient.createDownload(materialId, AUTH_HEADER))
                .thenReturn(ApiResponse.success(HttpStatus.OK.value(), "ok", new MaterialDownloadResponse(
                        materialId,
                        "Brief",
                        null,
                        "brief.pdf",
                        "application/pdf",
                        128L,
                        "PDF",
                        "ASSIGNMENT_ATTACHMENT",
                        "AVAILABLE",
                        teacherId,
                        courseId,
                        "checksum",
                        null,
                        null,
                        "https://download.example/brief",
                        expiresAt
                )));

        ApiResponse<AssignmentAttachmentDownloadResponse> response =
                attachmentService.downloadAttachment(assignmentId, attachmentId, AUTH_HEADER);

        assertThat(response.getData().getDownloadUrl()).isEqualTo("https://download.example/brief");
        assertThat(response.getData().getExpiresAt()).isEqualTo(expiresAt);
        assertThat(response.getData().getContent()).isNull();
        verify(fileStorageService, never()).openStream(any());
    }

    @Test
    void downloadAttachment_whenLegacyLocalAttachment_returnsStoredContent() {
        UUID assignmentId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        Assignment assignment = assignment(assignmentId, UUID.randomUUID(), teacherId, AssignmentStatus.DRAFT);
        AssignmentAttachment attachment = AssignmentAttachment.builder()
                .assignment(assignment)
                .storedFilename("legacy.pdf")
                .originalFilename("legacy.pdf")
                .contentType("application/pdf")
                .fileSize(5)
                .uploadedBy(teacherId)
                .build();
        attachment.setId(attachmentId);

        when(assignmentRepository.findByIdAndIsActiveTrue(assignmentId)).thenReturn(Optional.of(assignment));
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.getCurrentUserId()).thenReturn(teacherId);
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(true);
        when(attachmentRepository.findByIdAndAssignmentIdAndIsActiveTrue(attachmentId, assignmentId))
                .thenReturn(Optional.of(attachment));
        when(fileStorageService.openStream("legacy.pdf")).thenReturn(new ByteArrayInputStream("hello".getBytes()));

        ApiResponse<AssignmentAttachmentDownloadResponse> response =
                attachmentService.downloadAttachment(assignmentId, attachmentId, AUTH_HEADER);

        assertThat(response.getData().getDownloadUrl()).isNull();
        assertThat(response.getData().getContent()).isEqualTo("hello".getBytes());
    }

    private Assignment assignment(UUID assignmentId, UUID courseId, UUID teacherId, AssignmentStatus status) {
        Assignment assignment = Assignment.builder()
                .courseId(courseId)
                .moduleId(UUID.randomUUID())
                .teacherId(teacherId)
                .title("Essay")
                .status(status)
                .build();
        assignment.setId(assignmentId);
        return assignment;
    }

    private AssignmentAttachment materialAttachment(Assignment assignment, UUID materialId) {
        AssignmentAttachment attachment = AssignmentAttachment.builder()
                .assignment(assignment)
                .materialId(materialId)
                .originalFilename("brief.pdf")
                .contentType("application/pdf")
                .fileSize(128)
                .uploadedBy(assignment.getTeacherId())
                .build();
        attachment.setId(UUID.randomUUID());
        return attachment;
    }

    private MaterialLookupResponse material(UUID materialId, UUID courseId) {
        return new MaterialLookupResponse(
                materialId,
                "Brief",
                null,
                "brief.pdf",
                "application/pdf",
                128L,
                "PDF",
                "ASSIGNMENT_ATTACHMENT",
                "AVAILABLE",
                UUID.randomUUID(),
                courseId,
                "checksum",
                null,
                null
        );
    }

    private ApiResponse<CourseLookupResponse> courseResponse(UUID courseId, UUID teacherId) {
        CourseLookupResponse course = new CourseLookupResponse();
        course.setId(courseId);
        course.setAssignedTeacherId(teacherId);
        return ApiResponse.success(HttpStatus.OK.value(), "ok", course);
    }
}
