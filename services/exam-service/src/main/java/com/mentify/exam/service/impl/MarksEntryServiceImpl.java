package com.mentify.exam.service.impl;

import com.mentify.exam.dto.request.BatchMarksEntryRequest;
import com.mentify.exam.dto.request.MarkEntryRequest;
import com.mentify.exam.dto.response.ExamResultResponse;
import com.mentify.exam.dto.response.MarksEntryResponse;
import com.mentify.exam.entity.Exam;
import com.mentify.exam.entity.ExamResult;
import com.mentify.exam.enums.AttendanceStatus;
import com.mentify.exam.enums.ExamStatus;
import com.mentify.exam.enums.ResultStatus;
import com.mentify.exam.exception.ExamDomainException;
import com.mentify.exam.repository.ExamRepository;
import com.mentify.exam.repository.ExamResultRepository;
import com.mentify.exam.security.CurrentUserService;
import com.mentify.exam.service.CourseAccessService;
import com.mentify.exam.service.MarksEntryService;
import com.mentify.common.notification.NotificationEventPublisher;
import com.mentify.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarksEntryServiceImpl implements MarksEntryService {

    private final ExamRepository examRepository;
    private final ExamResultRepository examResultRepository;
    private final CourseAccessService courseAccessService;
    private final CurrentUserService currentUserService;

    @Autowired(required = false)
    private NotificationEventPublisher notificationEventPublisher;

    @Override
    @Transactional
    public MarksEntryResponse enterMarks(UUID examId, BatchMarksEntryRequest request, String authorizationHeader) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", "id", examId));
        var course = courseAccessService.assertCanManage(exam.getCourseId(), authorizationHeader);
        assertMarkingAllowed(exam.getStatus());

        List<MarkEntryRequest> entries = request.getResults();
        assertNoDuplicates(entries);
        entries.forEach(entry -> validateEntry(entry, exam));

        Map<UUID, ExamResult> existing = examResultRepository.findByExamId(examId).stream()
                .collect(Collectors.toMap(ExamResult::getStudentId, Function.identity()));
        for (MarkEntryRequest entry : entries) {
            if (!existing.containsKey(entry.getStudentId())) {
                throw new ExamDomainException(HttpStatus.BAD_REQUEST,
                        "Student " + entry.getStudentId() + " has no initialized result for this exam");
            }
        }

        UUID markedBy = currentUserService.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        List<ExamResult> updated = entries.stream()
                .map(entry -> apply(existing.get(entry.getStudentId()), entry, exam, markedBy, now))
                .toList();
        List<ExamResult> saved = examResultRepository.saveAll(updated);

        if (notificationEventPublisher != null) {
            saved.stream()
                    .filter(result -> result.getStudentId() != null)
                    .forEach(result -> notificationEventPublisher.publishForRecipient(
                            result.getStudentId().toString(),
                            null,
                            "EXAM_RESULTS_RECORDED",
                            "Exam result updated",
                            "Your result is ready for \"" + exam.getTitle() + "\" in "
                                    + course.getCourseName()
                                    + (course.getCourseCode() == null || course.getCourseCode().isBlank()
                                    ? "" : " (" + course.getCourseCode() + ")")
                                    + ". Exam date: " + exam.getExamDate() + "; time: " + exam.getStartTime()
                                    + "–" + exam.getEndTime() + "; venue: "
                                    + (exam.getVenue() == null || exam.getVenue().isBlank()
                                    ? "not specified" : exam.getVenue())
                                    + ". Marks: " + (result.getMarksObtained() == null
                                    ? "not awarded" : result.getMarksObtained() + " / " + exam.getTotalMarks())
                                    + "; grade: " + (result.getGrade() == null ? "not assigned" : result.getGrade())
                                    + "; result status: " + result.getResultStatus()
                                    + (result.getAttendanceStatus() == null ? "" : "; attendance: " + result.getAttendanceStatus())
                                    + (result.getRemarks() == null || result.getRemarks().isBlank()
                                    ? "" : "; teacher remarks: " + result.getRemarks())
                                    + ". Updated: " + (result.getMarkedAt() == null ? now : result.getMarkedAt())
                                    + ". Open Examination Results to review this result."
                    ));
        }

        return MarksEntryResponse.builder()
                .examId(examId)
                .updatedCount(saved.size())
                .results(saved.stream().map(this::toResponse).toList())
                .build();
    }

    private void assertMarkingAllowed(ExamStatus status) {
        if (status != ExamStatus.MARKING) {
            throw new ExamDomainException(HttpStatus.CONFLICT,
                    "Marks can only be entered while the exam is in MARKING status (current: " + status + ")");
        }
    }

    private void assertNoDuplicates(List<MarkEntryRequest> entries) {
        Set<UUID> seen = new HashSet<>();
        for (MarkEntryRequest entry : entries) {
            if (!seen.add(entry.getStudentId())) {
                throw new ExamDomainException(HttpStatus.BAD_REQUEST,
                        "Duplicate student " + entry.getStudentId() + " in batch");
            }
        }
    }

    private void validateEntry(MarkEntryRequest entry, Exam exam) {
        BigDecimal marks = entry.getMarksObtained();
        if (entry.getAttendanceStatus() == AttendanceStatus.ABSENT) {
            if (marks != null) {
                throw new ExamDomainException(HttpStatus.BAD_REQUEST,
                        "marksObtained must be null for absent student " + entry.getStudentId());
            }
            return;
        }
        if (marks == null) {
            throw new ExamDomainException(HttpStatus.BAD_REQUEST,
                    "marksObtained is required for present student " + entry.getStudentId());
        }
        if (marks.signum() < 0) {
            throw new ExamDomainException(HttpStatus.BAD_REQUEST,
                    "marksObtained must not be negative for student " + entry.getStudentId());
        }
        if (marks.compareTo(exam.getTotalMarks()) > 0) {
            throw new ExamDomainException(HttpStatus.BAD_REQUEST,
                    "marksObtained must not exceed totalMarks for student " + entry.getStudentId());
        }
    }

    private ExamResult apply(ExamResult result, MarkEntryRequest entry, Exam exam, UUID markedBy, LocalDateTime now) {
        result.setAttendanceStatus(entry.getAttendanceStatus());
        result.setRemarks(entry.getRemarks());
        result.setMarkedBy(markedBy);
        result.setMarkedAt(now);
        if (entry.getAttendanceStatus() == AttendanceStatus.PRESENT) {
            result.setMarksObtained(entry.getMarksObtained());
            result.setResultStatus(entry.getMarksObtained().compareTo(exam.getPassMarks()) >= 0
                    ? ResultStatus.PASS : ResultStatus.FAIL);
            result.setGrade(gradeFor(entry.getMarksObtained(), exam.getTotalMarks()));
        } else {
            result.setMarksObtained(null);
            result.setResultStatus(ResultStatus.NOT_MARKED);
            result.setGrade(null);
        }
        return result;
    }

    private String gradeFor(BigDecimal marks, BigDecimal totalMarks) {
        BigDecimal percentage = marks.multiply(BigDecimal.valueOf(100)).divide(totalMarks, 2,
                java.math.RoundingMode.HALF_UP);
        if (percentage.compareTo(BigDecimal.valueOf(90)) >= 0) return "A";
        if (percentage.compareTo(BigDecimal.valueOf(80)) >= 0) return "B";
        if (percentage.compareTo(BigDecimal.valueOf(70)) >= 0) return "C";
        if (percentage.compareTo(BigDecimal.valueOf(60)) >= 0) return "D";
        return "F";
    }

    private ExamResultResponse toResponse(ExamResult r) {
        return ExamResultResponse.builder()
                .studentId(r.getStudentId())
                .attendanceStatus(r.getAttendanceStatus())
                .marksObtained(r.getMarksObtained())
                .resultStatus(r.getResultStatus())
                .grade(r.getGrade())
                .remarks(r.getRemarks())
                .markedBy(r.getMarkedBy())
                .markedAt(r.getMarkedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
