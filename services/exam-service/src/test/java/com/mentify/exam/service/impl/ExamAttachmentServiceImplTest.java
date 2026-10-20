package com.mentify.exam.service.impl;

import com.mentify.exam.client.EnrollmentServiceClient;
import com.mentify.exam.client.MaterialInternalServiceClient;
import com.mentify.exam.client.MaterialServiceClient;
import com.mentify.exam.client.dto.MaterialUploadResponse;
import com.mentify.exam.entity.Exam;
import com.mentify.exam.entity.ExamAttachment;
import com.mentify.exam.enums.ExamStatus;
import com.mentify.exam.repository.ExamAttachmentRepository;
import com.mentify.exam.repository.ExamRepository;
import com.mentify.exam.security.CurrentUserService;
import com.mentify.exam.service.CourseAccessService;
import com.mentify.payload.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
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
class ExamAttachmentServiceImplTest {

    private static final String AUTH = "Bearer token";

    @Mock private ExamRepository examRepository;
    @Mock private ExamAttachmentRepository attachmentRepository;
    @Mock private MaterialServiceClient materialServiceClient;
    @Mock private MaterialInternalServiceClient materialInternalServiceClient;
    @Mock private CourseAccessService courseAccessService;
    @Mock private EnrollmentServiceClient enrollmentServiceClient;
    @Mock private CurrentUserService currentUserService;

    private ExamAttachmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ExamAttachmentServiceImpl(examRepository, attachmentRepository,
                materialServiceClient, materialInternalServiceClient, courseAccessService,
                enrollmentServiceClient, currentUserService);
    }

    @Test
    void upload_associatesMaterialAndPersistsExamReference() {
        UUID examId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        Exam exam = exam(examId, courseId, teacherId, ExamStatus.DRAFT);
        MockMultipartFile file = new MockMultipartFile("file", "instructions.pdf", "application/pdf", "pdf".getBytes());

        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(currentUserService.getCurrentUserId()).thenReturn(teacherId);
        when(materialServiceClient.upload("instructions.pdf", null, "EXAM_ATTACHMENT", courseId, file, AUTH))
                .thenReturn(ApiResponse.success(HttpStatus.CREATED.value(), "uploaded", new MaterialUploadResponse(
                        materialId, "instructions.pdf", "instructions.pdf", "application/pdf", 3L,
                        "PDF", "EXAM_ATTACHMENT", "AVAILABLE", teacherId, courseId, "checksum", null, null)));
        when(attachmentRepository.save(any(ExamAttachment.class))).thenAnswer(invocation -> {
            ExamAttachment attachment = invocation.getArgument(0);
            attachment.setId(UUID.randomUUID());
            return attachment;
        });

        var response = service.upload(examId, file, true, AUTH);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getData().getMaterialId()).isEqualTo(materialId);
        assertThat(response.getData().isStudentVisible()).isTrue();
        verify(materialInternalServiceClient).associate(any(), any(), any());
    }

    @Test
    void upload_rejectsNonPdfFilesBeforeCallingMaterialService() {
        UUID examId = UUID.randomUUID();
        Exam exam = exam(examId, UUID.randomUUID(), UUID.randomUUID(), ExamStatus.DRAFT);
        MockMultipartFile file = new MockMultipartFile("file", "instructions.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "content".getBytes());

        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        assertThatThrownBy(() -> service.upload(examId, file, true, AUTH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Exam instructions must be a PDF file");
        verify(materialServiceClient, never()).upload(any(), any(), any(), any(), any(), any());
    }

    @Test
    void list_forStudentHidesConfidentialAttachments() {
        UUID examId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Exam exam = exam(examId, courseId, UUID.randomUUID(), ExamStatus.SCHEDULED);
        ExamAttachment confidential = ExamAttachment.builder()
                .id(UUID.randomUUID()).examId(examId).materialId(UUID.randomUUID())
                .originalFilename("answer-key.pdf").contentType("application/pdf")
                .fileSize(10).uploadedBy(exam.getCreatedBy()).studentVisible(false).active(true).build();

        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(currentUserService.hasAnyRole(com.mentify.exam.enums.ExamRole.ADMIN,
                com.mentify.exam.enums.ExamRole.SUPER_ADMIN,
                com.mentify.exam.enums.ExamRole.TEACHER)).thenReturn(false);
        when(currentUserService.hasAnyRole(com.mentify.exam.enums.ExamRole.STUDENT)).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(enrollmentServiceClient.isStudentEnrolledInCourse(studentId, courseId)).thenReturn(true);
        when(attachmentRepository.findByExamIdAndActiveTrueOrderByCreatedAtAsc(examId))
                .thenReturn(List.of(confidential));

        var response = service.list(examId, AUTH);

        assertThat(response.getData()).isEmpty();
        verify(materialServiceClient, never()).createDownload(any(), any());
    }

    @Test
    void list_forStudentBeforeExamIsDenied() {
        UUID examId = UUID.randomUUID();
        Exam exam = exam(examId, UUID.randomUUID(), UUID.randomUUID(), ExamStatus.DRAFT);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(currentUserService.hasAnyRole(com.mentify.exam.enums.ExamRole.ADMIN,
                com.mentify.exam.enums.ExamRole.SUPER_ADMIN,
                com.mentify.exam.enums.ExamRole.TEACHER)).thenReturn(false);
        when(currentUserService.hasAnyRole(com.mentify.exam.enums.ExamRole.STUDENT)).thenReturn(true);

        assertThatThrownBy(() -> service.list(examId, AUTH)).isInstanceOf(AccessDeniedException.class);
    }

    private Exam exam(UUID id, UUID courseId, UUID teacherId, ExamStatus status) {
        return Exam.builder().id(id).courseId(courseId).title("Exam").description("Description")
                .examDate(LocalDate.now()).startTime(LocalTime.NOON).endTime(LocalTime.NOON.plusHours(1))
                .totalMarks(BigDecimal.TEN).passMarks(BigDecimal.ONE)
                .createdBy(teacherId).status(status).build();
    }
}
