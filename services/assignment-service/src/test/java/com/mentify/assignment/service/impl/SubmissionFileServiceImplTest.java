package com.mentify.assignment.service.impl;

import com.mentify.assignment.client.MaterialServiceClient;
import com.mentify.assignment.client.dto.MaterialDownloadResponse;
import com.mentify.assignment.client.dto.MaterialLookupResponse;
import com.mentify.assignment.dto.request.AttachSubmissionMaterialRequest;
import com.mentify.assignment.dto.response.SubmissionFileDownloadResponse;
import com.mentify.assignment.dto.response.SubmissionFileResponse;
import com.mentify.assignment.entity.Assignment;
import com.mentify.assignment.entity.Submission;
import com.mentify.assignment.entity.SubmissionFile;
import com.mentify.assignment.enums.SubmissionStatus;
import com.mentify.assignment.repository.SubmissionFileRepository;
import com.mentify.assignment.repository.SubmissionRepository;
import com.mentify.assignment.storage.FileStorageService;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
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
class SubmissionFileServiceImplTest {

    private static final String AUTH_HEADER = "Bearer token";

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private SubmissionFileRepository fileRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private MaterialServiceClient materialServiceClient;

    @Mock
    private CurrentUserService currentUserService;

    private SubmissionFileServiceImpl submissionFileService;

    @BeforeEach
    void setUp() {
        submissionFileService = new SubmissionFileServiceImpl(
                submissionRepository,
                fileRepository,
                fileStorageService,
                materialServiceClient,
                currentUserService
        );
    }

    @Test
    void attachMaterial_whenStudentOwnsSubmission_persistsMaterialReference() {
        UUID courseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        Submission submission = submission(courseId, UUID.randomUUID(), studentId, SubmissionStatus.DRAFT);
        MaterialLookupResponse material = material(materialId, courseId, studentId);

        when(submissionRepository.findByIdAndIsActiveTrue(submission.getId())).thenReturn(Optional.of(submission));
        when(currentUserService.hasAnyRole("STUDENT")).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(materialServiceClient.getMaterial(materialId, AUTH_HEADER))
                .thenReturn(ApiResponse.success(HttpStatus.OK.value(), "ok", material));
        when(fileRepository.save(any(SubmissionFile.class))).thenAnswer(invocation -> {
            SubmissionFile file = invocation.getArgument(0);
            file.setId(UUID.randomUUID());
            return file;
        });

        ApiResponse<SubmissionFileResponse> response = submissionFileService.attachMaterial(
                submission.getId(),
                AttachSubmissionMaterialRequest.builder().materialId(materialId).build(),
                AUTH_HEADER
        );

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getData().getMaterialId()).isEqualTo(materialId);
        assertThat(response.getData().isMaterialBacked()).isTrue();

        ArgumentCaptor<SubmissionFile> captor = ArgumentCaptor.forClass(SubmissionFile.class);
        verify(fileRepository).save(captor.capture());
        assertThat(captor.getValue().getMaterialId()).isEqualTo(materialId);
        assertThat(captor.getValue().getStoredFilename()).isEqualTo("material:" + materialId);
        assertThat(captor.getValue().getOriginalFilename()).isEqualTo("answer.pdf");
        verify(fileStorageService, never()).openStream(any());
        verify(fileStorageService, never()).delete(any());
    }

    @Test
    void attachMaterial_whenStudentDoesNotOwnSubmission_throwsAccessDenied() {
        UUID ownerStudentId = UUID.randomUUID();
        UUID currentStudentId = UUID.randomUUID();
        Submission submission = submission(UUID.randomUUID(), UUID.randomUUID(), ownerStudentId, SubmissionStatus.DRAFT);

        when(submissionRepository.findByIdAndIsActiveTrue(submission.getId())).thenReturn(Optional.of(submission));
        when(currentUserService.hasAnyRole("STUDENT")).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(currentStudentId);

        assertThatThrownBy(() -> submissionFileService.attachMaterial(
                submission.getId(),
                AttachSubmissionMaterialRequest.builder().materialId(UUID.randomUUID()).build(),
                AUTH_HEADER
        )).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void attachMaterial_whenMaterialIdIsInvalid_throwsNotFound() {
        UUID courseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        Submission submission = submission(courseId, UUID.randomUUID(), studentId, SubmissionStatus.DRAFT);

        when(submissionRepository.findByIdAndIsActiveTrue(submission.getId())).thenReturn(Optional.of(submission));
        when(currentUserService.hasAnyRole("STUDENT")).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(materialServiceClient.getMaterial(materialId, AUTH_HEADER))
                .thenReturn(ApiResponse.success(HttpStatus.OK.value(), "ok", null));

        assertThatThrownBy(() -> submissionFileService.attachMaterial(
                submission.getId(),
                AttachSubmissionMaterialRequest.builder().materialId(materialId).build(),
                AUTH_HEADER
        )).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Material not found with id: '" + materialId + "'");
    }

    @Test
    void attachMaterial_whenMaterialUploadedByAnotherStudent_throwsAccessDenied() {
        UUID courseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        Submission submission = submission(courseId, UUID.randomUUID(), studentId, SubmissionStatus.DRAFT);
        MaterialLookupResponse material = material(materialId, courseId, UUID.randomUUID());

        when(submissionRepository.findByIdAndIsActiveTrue(submission.getId())).thenReturn(Optional.of(submission));
        when(currentUserService.hasAnyRole("STUDENT")).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(materialServiceClient.getMaterial(materialId, AUTH_HEADER))
                .thenReturn(ApiResponse.success(HttpStatus.OK.value(), "ok", material));

        assertThatThrownBy(() -> submissionFileService.attachMaterial(
                submission.getId(),
                AttachSubmissionMaterialRequest.builder().materialId(materialId).build(),
                AUTH_HEADER
        )).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void listFiles_whenTeacherOwnsAssignment_returnsFiles() {
        UUID teacherId = UUID.randomUUID();
        Submission submission = submission(UUID.randomUUID(), teacherId, UUID.randomUUID(), SubmissionStatus.SUBMITTED);
        SubmissionFile file = materialFile(submission, UUID.randomUUID());

        when(submissionRepository.findByIdAndIsActiveTrue(submission.getId())).thenReturn(Optional.of(submission));
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.getCurrentUserId()).thenReturn(teacherId);
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(true);
        when(fileRepository.findBySubmissionIdAndIsActiveTrueOrderByCreatedAtAsc(submission.getId()))
                .thenReturn(List.of(file));

        ApiResponse<List<SubmissionFileResponse>> response =
                submissionFileService.listFiles(submission.getId(), AUTH_HEADER);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getMaterialId()).isEqualTo(file.getMaterialId());
    }

    @Test
    void listFiles_whenUnauthorizedStudent_throwsAccessDenied() {
        Submission submission = submission(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), SubmissionStatus.SUBMITTED);

        when(submissionRepository.findByIdAndIsActiveTrue(submission.getId())).thenReturn(Optional.of(submission));
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(false);
        when(currentUserService.hasAnyRole("STUDENT")).thenReturn(true);

        assertThatThrownBy(() -> submissionFileService.listFiles(submission.getId(), AUTH_HEADER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void downloadFile_whenMaterialBacked_returnsMaterialDownloadUrl() {
        UUID teacherId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        Submission submission = submission(UUID.randomUUID(), teacherId, UUID.randomUUID(), SubmissionStatus.SUBMITTED);
        SubmissionFile file = materialFile(submission, materialId);
        file.setId(fileId);
        Instant expiresAt = Instant.now().plusSeconds(300);

        when(submissionRepository.findByIdAndIsActiveTrue(submission.getId())).thenReturn(Optional.of(submission));
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.getCurrentUserId()).thenReturn(teacherId);
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(true);
        when(fileRepository.findByIdAndSubmissionIdAndIsActiveTrue(fileId, submission.getId()))
                .thenReturn(Optional.of(file));
        when(materialServiceClient.createDownload(materialId, AUTH_HEADER))
                .thenReturn(ApiResponse.success(HttpStatus.OK.value(), "ok", new MaterialDownloadResponse(
                        materialId,
                        "Answer",
                        null,
                        "answer.pdf",
                        "application/pdf",
                        256L,
                        "PDF",
                        "SUBMISSION_ATTACHMENT",
                        "AVAILABLE",
                        submission.getStudentId(),
                        submission.getAssignment().getCourseId(),
                        "checksum",
                        null,
                        null,
                        "https://download.example/answer",
                        expiresAt
                )));

        ApiResponse<SubmissionFileDownloadResponse> response =
                submissionFileService.downloadFile(submission.getId(), fileId, AUTH_HEADER);

        assertThat(response.getData().getDownloadUrl()).isEqualTo("https://download.example/answer");
        assertThat(response.getData().getExpiresAt()).isEqualTo(expiresAt);
        assertThat(response.getData().getContent()).isNull();
        verify(fileStorageService, never()).openStream(any());
    }

    @Test
    void downloadFile_whenLegacyLocalFile_returnsStoredContent() {
        UUID studentId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        Submission submission = submission(UUID.randomUUID(), UUID.randomUUID(), studentId, SubmissionStatus.SUBMITTED);
        SubmissionFile file = SubmissionFile.builder()
                .submission(submission)
                .storedFilename("legacy-answer.pdf")
                .originalFilename("legacy-answer.pdf")
                .contentType("application/pdf")
                .fileSize(5)
                .build();
        file.setId(fileId);

        when(submissionRepository.findByIdAndIsActiveTrue(submission.getId())).thenReturn(Optional.of(submission));
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(false);
        when(currentUserService.hasAnyRole("STUDENT")).thenReturn(true);
        when(fileRepository.findByIdAndSubmissionIdAndIsActiveTrue(fileId, submission.getId()))
                .thenReturn(Optional.of(file));
        when(fileStorageService.openStream("legacy-answer.pdf")).thenReturn(new ByteArrayInputStream("hello".getBytes()));

        ApiResponse<SubmissionFileDownloadResponse> response =
                submissionFileService.downloadFile(submission.getId(), fileId, AUTH_HEADER);

        assertThat(response.getData().getDownloadUrl()).isNull();
        assertThat(response.getData().getContent()).isEqualTo("hello".getBytes());
    }

    private Submission submission(UUID courseId, UUID teacherId, UUID studentId, SubmissionStatus status) {
        Assignment assignment = Assignment.builder()
                .courseId(courseId)
                .moduleId(UUID.randomUUID())
                .teacherId(teacherId)
                .title("Essay")
                .build();
        assignment.setId(UUID.randomUUID());

        Submission submission = Submission.builder()
                .assignment(assignment)
                .studentId(studentId)
                .attemptNumber(1)
                .status(status)
                .build();
        submission.setId(UUID.randomUUID());
        return submission;
    }

    private SubmissionFile materialFile(Submission submission, UUID materialId) {
        SubmissionFile file = SubmissionFile.builder()
                .submission(submission)
                .materialId(materialId)
                .storedFilename("material:" + materialId)
                .originalFilename("answer.pdf")
                .contentType("application/pdf")
                .fileSize(256)
                .build();
        file.setId(UUID.randomUUID());
        return file;
    }

    private MaterialLookupResponse material(UUID materialId, UUID courseId, UUID uploadedBy) {
        return new MaterialLookupResponse(
                materialId,
                "Answer",
                null,
                "answer.pdf",
                "application/pdf",
                256L,
                "PDF",
                "SUBMISSION_ATTACHMENT",
                "AVAILABLE",
                uploadedBy,
                courseId,
                "checksum",
                null,
                null
        );
    }
}
