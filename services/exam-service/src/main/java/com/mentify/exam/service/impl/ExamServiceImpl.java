package com.mentify.exam.service.impl;

import com.mentify.exam.dto.request.CreateExamRequest;
import com.mentify.exam.dto.request.UpdateExamRequest;
import com.mentify.exam.dto.response.ExamResponse;
import com.mentify.exam.entity.Exam;
import com.mentify.exam.entity.ExamResult;
import com.mentify.exam.enums.AttendanceStatus;
import com.mentify.exam.enums.ResultStatus;
import com.mentify.exam.repository.ExamResultRepository;
import com.mentify.exam.service.ExamResultService;
import com.mentify.exam.enums.ExamStatus;
import com.mentify.exam.exception.ExamDomainException;
import com.mentify.exam.mapper.ExamMapper;
import com.mentify.exam.repository.ExamRepository;
import com.mentify.exam.security.CurrentUserService;
import com.mentify.exam.service.CourseAccessService;
import com.mentify.exam.service.ExamService;
import com.mentify.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private static final Set<ExamStatus> EDITABLE = EnumSet.of(ExamStatus.DRAFT, ExamStatus.SCHEDULED);
    private static final Set<ExamStatus> CANCELLABLE = EnumSet.of(ExamStatus.DRAFT, ExamStatus.SCHEDULED);

    private final ExamRepository examRepository;
    private final ExamResultRepository examResultRepository;
    private final ExamMapper examMapper;
    private final CourseAccessService courseAccessService;
    private final CurrentUserService currentUserService;
    private final ExamResultService examResultService;

    @Override
    @Transactional
    public ExamResponse createExam(CreateExamRequest request, String authorizationHeader) {
        courseAccessService.assertCanManage(request.getCourseId(), authorizationHeader);
        validateMarksAndTimes(request.getTotalMarks(), request.getPassMarks(),
                request.getStartTime(), request.getEndTime());
        Exam exam = examMapper.toEntity(request, currentUserService.getCurrentUserId());
        return examMapper.toResponse(examRepository.save(exam));
    }

    @Override
    @Transactional(readOnly = true)
    public ExamResponse getExam(UUID examId, String authorizationHeader) {
        Exam exam = findExam(examId);
        courseAccessService.assertCanManage(exam.getCourseId(), authorizationHeader);
        return examMapper.toResponse(exam);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamResponse> getExamsByCourse(UUID courseId, String authorizationHeader) {
        courseAccessService.assertCanManage(courseId, authorizationHeader);
        return examRepository.findByCourseId(courseId).stream().map(examMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public ExamResponse updateExam(UUID examId, UpdateExamRequest request, String authorizationHeader) {
        Exam exam = findExam(examId);
        courseAccessService.assertCanManage(exam.getCourseId(), authorizationHeader);
        if (!EDITABLE.contains(exam.getStatus())) {
            throw new ExamDomainException(HttpStatus.CONFLICT,
                    "Exam in status " + exam.getStatus() + " cannot be modified");
        }
        validateMarksAndTimes(request.getTotalMarks(), request.getPassMarks(),
                request.getStartTime(), request.getEndTime());
        examMapper.applyUpdate(exam, request);
        return examMapper.toResponse(examRepository.save(exam));
    }

    @Override
    @Transactional
    public ExamResponse cancelExam(UUID examId, String authorizationHeader) {
        Exam exam = findExam(examId);
        courseAccessService.assertCanManage(exam.getCourseId(), authorizationHeader);
        if (!CANCELLABLE.contains(exam.getStatus())) {
            throw new ExamDomainException(HttpStatus.CONFLICT,
                    "Exam in status " + exam.getStatus() + " cannot be cancelled");
        }
        exam.setStatus(ExamStatus.CANCELLED);
        return examMapper.toResponse(examRepository.save(exam));
    }

    @Override
    @Transactional
    public ExamResponse scheduleExam(UUID examId, String authorizationHeader) {
        Exam exam = findExam(examId);
        courseAccessService.assertCanManage(exam.getCourseId(), authorizationHeader);
        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw new ExamDomainException(HttpStatus.CONFLICT,
                    "Exam in status " + exam.getStatus() + " cannot be scheduled");
        }
        examResultService.initializeResults(examId, authorizationHeader);
        exam.setStatus(ExamStatus.SCHEDULED);
        return examMapper.toResponse(examRepository.save(exam));
    }

    @Override
    @Transactional
    public ExamResponse startMarking(UUID examId, String authorizationHeader) {
        Exam exam = findExam(examId);
        courseAccessService.assertCanManage(exam.getCourseId(), authorizationHeader);
        if (exam.getStatus() != ExamStatus.SCHEDULED) {
            throw new ExamDomainException(HttpStatus.CONFLICT,
                    "Exam in status " + exam.getStatus() + " cannot be moved to marking");
        }
        // Picks up students who enrolled after scheduling; existing rows are untouched.
        examResultService.initializeResults(examId, authorizationHeader);
        exam.setStatus(ExamStatus.MARKING);
        return examMapper.toResponse(examRepository.save(exam));
    }

    @Override
    @Transactional
    public ExamResponse completeExam(UUID examId, String authorizationHeader) {
        Exam exam = findExam(examId);
        courseAccessService.assertCanManage(exam.getCourseId(), authorizationHeader);
        if (exam.getStatus() != ExamStatus.MARKING) {
            throw new ExamDomainException(HttpStatus.CONFLICT,
                    "Exam in status " + exam.getStatus() + " cannot be completed");
        }
        List<ExamResult> results = examResultRepository.findByExamId(examId);
        if (results.isEmpty()) {
            throw new ExamDomainException(HttpStatus.CONFLICT,
                    "Exam cannot be completed because no student results are initialized");
        }
        long unresolved = results.stream().filter(r -> !isResolved(r, exam)).count();
        if (unresolved > 0) {
            throw new ExamDomainException(HttpStatus.CONFLICT,
                    "Exam cannot be completed: " + unresolved + " student result(s) are unresolved");
        }
        exam.setStatus(ExamStatus.COMPLETED);
        return examMapper.toResponse(examRepository.save(exam));
    }

    private boolean isResolved(ExamResult result, Exam exam) {
        if (result.getAttendanceStatus() == AttendanceStatus.ABSENT) {
            return result.getMarksObtained() == null;
        }
        if (result.getAttendanceStatus() == AttendanceStatus.PRESENT) {
            java.math.BigDecimal marks = result.getMarksObtained();
            return marks != null
                    && marks.signum() >= 0
                    && marks.compareTo(exam.getTotalMarks()) <= 0
                    && (result.getResultStatus() == ResultStatus.PASS
                    || result.getResultStatus() == ResultStatus.FAIL);
        }
        return false;
    }

    private Exam findExam(UUID examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", "id", examId));
    }

    private void validateMarksAndTimes(java.math.BigDecimal totalMarks, java.math.BigDecimal passMarks,
                                       java.time.LocalTime start, java.time.LocalTime end) {
        if (totalMarks.signum() <= 0) {
            throw new ExamDomainException(HttpStatus.BAD_REQUEST, "totalMarks must be greater than 0");
        }
        if (passMarks.signum() < 0 || passMarks.compareTo(totalMarks) > 0) {
            throw new ExamDomainException(HttpStatus.BAD_REQUEST,
                    "passMarks must be between 0 and totalMarks");
        }
        if (!end.isAfter(start)) {
            throw new ExamDomainException(HttpStatus.BAD_REQUEST, "endTime must be after startTime");
        }
    }
}
