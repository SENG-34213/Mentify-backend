package com.mentify.service.impl;

import com.mentify.dto.CourseProgressResponse;
import com.mentify.dto.LessonProgressResponse;
import com.mentify.dto.LessonProgressUpdateRequest;
import com.mentify.entity.Course;
import com.mentify.entity.Lesson;
import com.mentify.entity.StudentLessonProgress;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.repository.CourseRepository;
import com.mentify.repository.LessonRepository;
import com.mentify.repository.StudentLessonProgressRepository;
import com.mentify.service.StudentProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentProgressServiceImpl implements StudentProgressService {

    private final StudentLessonProgressRepository progressRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final CourseContentAccessGuard courseContentAccessGuard;

    @Override
    @Transactional(readOnly = true)
    public List<LessonProgressResponse> getMyProgress() {
        UUID studentId = currentStudentId();
        return progressRepository.findAllByStudentIdOrderByLastAccessedAtDesc(studentId).stream()
                .map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CourseProgressResponse getCourseProgress(UUID courseId, String authorizationHeader) {
        UUID studentId = currentStudentId();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        courseContentAccessGuard.assertCanReadCourseContent(course, authorizationHeader);
        List<Lesson> lessons = lessonRepository.findAllByModule_Course_Id(courseId).stream()
                .filter(Lesson::isActive).toList();
        List<StudentLessonProgress> records = progressRepository
                .findAllByStudentIdAndCourseIdOrderByLastAccessedAtDesc(studentId, courseId);
        long completed = records.stream().filter(StudentLessonProgress::isCompleted).count();
        StudentLessonProgress last = records.isEmpty() ? null : records.get(0);
        int total = lessons.size();
        return CourseProgressResponse.builder()
                .courseId(courseId)
                .completedLessons((int) completed)
                .totalLessons(total)
                .progressPercentage(total == 0 ? 0 : (int) Math.round(completed * 100.0 / total))
                .lastAccessedLessonId(last == null ? null : last.getLessonId())
                .lastAccessedAt(last == null ? null : last.getLastAccessedAt())
                .build();
    }

    @Override
    @Transactional
    public LessonProgressResponse updateLessonProgress(UUID lessonId, LessonProgressUpdateRequest request,
                                                       String authorizationHeader) {
        UUID studentId = currentStudentId();
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));
        UUID courseId = lesson.getModule().getCourse().getId();
        courseContentAccessGuard.assertCanReadCourseContent(lesson.getModule().getCourse(), authorizationHeader);
        StudentLessonProgress progress = progressRepository.findByStudentIdAndLessonId(studentId, lessonId)
                .orElseGet(() -> StudentLessonProgress.builder()
                        .studentId(studentId).courseId(courseId).lessonId(lessonId).build());
        progress.setCompleted(request.getCompleted());
        progress.setLastAccessedAt(LocalDateTime.now());
        return toResponse(progressRepository.save(progress));
    }

    private UUID currentStudentId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Jwt jwt) || jwt.getSubject() == null) {
            throw new AccessDeniedException("Authenticated student identity is missing");
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("Authenticated student identity is invalid");
        }
    }

    private LessonProgressResponse toResponse(StudentLessonProgress progress) {
        return LessonProgressResponse.builder()
                .lessonId(progress.getLessonId()).courseId(progress.getCourseId())
                .completed(progress.isCompleted()).lastAccessedAt(progress.getLastAccessedAt()).build();
    }
}
