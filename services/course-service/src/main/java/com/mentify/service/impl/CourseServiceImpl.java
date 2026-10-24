package com.mentify.service.impl;

import com.mentify.client.UserServiceClient;
import com.mentify.client.AssignmentDashboardClient;
import com.mentify.client.CalendarDashboardClient;
import com.mentify.client.ExamDashboardClient;
import com.mentify.client.QuizGradebookClient;
import com.mentify.client.EntrollmentServiceClient;
import com.mentify.client.dto.AssignmentDashboardResponse;
import com.mentify.client.dto.CalendarDashboardResponse;
import com.mentify.client.dto.ExamDashboardResponse;
import com.mentify.client.dto.SubmissionDashboardResponse;
import com.mentify.client.dashboard.DashboardDtos;
import com.mentify.dto.CourseRequest;
import com.mentify.dto.CourseResponse;
import com.mentify.dto.TeacherDashboardResponse;
import com.mentify.entity.Course;
import com.mentify.entity.Grade;
import com.mentify.exception.ResourceAlreadyExistsException;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.mapper.CourseMapper;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.CourseRepository;
import com.mentify.repository.GradeRepository;
import com.mentify.repository.CourseCountProjection;
import com.mentify.repository.ModuleRepository;
import com.mentify.repository.LessonRepository;
import com.mentify.service.CourseService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final GradeRepository gradeRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final UserServiceClient userServiceClient;
    private final TeacherCourseAccessGuard teacherCourseAccessGuard;
    private final EntrollmentServiceClient entrollmentServiceClient;
    private final AssignmentDashboardClient assignmentDashboardClient;
    private final ExamDashboardClient examDashboardClient;
    private final QuizGradebookClient quizGradebookClient;
    private final CalendarDashboardClient calendarDashboardClient;

    public CourseServiceImpl(CourseRepository courseRepository, GradeRepository gradeRepository,
                             UserServiceClient userServiceClient,
                             TeacherCourseAccessGuard teacherCourseAccessGuard) {
        this(courseRepository, gradeRepository, null, null, userServiceClient, teacherCourseAccessGuard,
                null, null, null, null, null);
    }

    @Autowired
    public CourseServiceImpl(CourseRepository courseRepository, GradeRepository gradeRepository,
                             ModuleRepository moduleRepository,
                             LessonRepository lessonRepository,
                             UserServiceClient userServiceClient,
                             TeacherCourseAccessGuard teacherCourseAccessGuard,
                             EntrollmentServiceClient entrollmentServiceClient,
                             AssignmentDashboardClient assignmentDashboardClient,
                             ExamDashboardClient examDashboardClient,
                             CalendarDashboardClient calendarDashboardClient,
                             QuizGradebookClient quizGradebookClient) {
        this.courseRepository = courseRepository;
        this.gradeRepository = gradeRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
        this.userServiceClient = userServiceClient;
        this.teacherCourseAccessGuard = teacherCourseAccessGuard;
        this.entrollmentServiceClient = entrollmentServiceClient;
        this.assignmentDashboardClient = assignmentDashboardClient;
        this.examDashboardClient = examDashboardClient;
        this.calendarDashboardClient = calendarDashboardClient;
        this.quizGradebookClient = quizGradebookClient;
    }

    @Override
    public ApiResponse<TeacherDashboardResponse> getTeacherDashboard(String authorizationHeader) {
        UUID teacherId = teacherCourseAccessGuard.isTeacherAuthenticated()
                ? teacherCourseAccessGuard.getAuthenticatedTeacherId() : null;
        List<Course> courses = teacherId == null
                ? courseRepository.findAll()
                : courseRepository.findAllByAssignedTeacherIdOrderByCreatedAtDesc(teacherId);

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        List<DashboardDtos.PendingGrading> pendingGrading = new ArrayList<>();
        Set<UUID> studentIds = new HashSet<>();
        long activeAssignments = 0;
        long pendingReviews = 0;
        List<DashboardDtos.CoursePerformance> performance = new ArrayList<>();

        for (Course course : courses) {
            List<UUID> enrolledStudents = enrolledStudents(course.getId(), authorizationHeader);
            studentIds.addAll(enrolledStudents);
            List<AssignmentDashboardResponse> assignments = assignments(course.getId(), authorizationHeader);
            long activeForCourse = assignments.stream()
                    .filter(a -> "PUBLISHED".equals(a.getStatus()))
                    .filter(a -> a.getDueDate() == null || !a.getDueDate().isBefore(now))
                    .count();
            activeAssignments += activeForCourse;
            long submissionCount = 0;
            long gradedCount = 0;
            for (AssignmentDashboardResponse assignment : assignments) {
                List<SubmissionDashboardResponse> submissions = submissions(assignment.getId(), authorizationHeader);
                submissionCount += submissions.size();
                gradedCount += submissions.stream().filter(s -> s.getStatus() != null
                        && Set.of("GRADED", "RETURNED").contains(s.getStatus())).count();
                for (SubmissionDashboardResponse submission : submissions) {
                    if (submission.getStatus() != null
                            && Set.of("SUBMITTED", "UNDER_REVIEW").contains(submission.getStatus())) {
                        pendingReviews++;
                        pendingGrading.add(new DashboardDtos.PendingGrading(
                                submission.getId(), assignment.getId(), course.getId(), assignment.getTitle(),
                                submission.getStudentId(), submission.getStudentName(), submission.getSubmittedAt()));
                    }
                }
            }
            double gradingRate = submissionCount == 0 ? 0.0 : (gradedCount * 100.0) / submissionCount;
            performance.add(new DashboardDtos.CoursePerformance(course.getId(), course.getCourseName(),
                    enrolledStudents.size(), activeForCourse, submissionCount, gradedCount, gradingRate));
        }

        List<DashboardDtos.Exam> upcomingExams = new ArrayList<>();
        for (Course course : courses) {
            for (ExamDashboardResponse exam : exams(course.getId(), authorizationHeader)) {
                if (exam.getExamDate() != null && !exam.getExamDate().isBefore(today)
                        && Set.of("SCHEDULED", "MARKING").contains(exam.getStatus())) {
                    upcomingExams.add(new DashboardDtos.Exam(exam.getId(), exam.getCourseId(), exam.getTitle(),
                            exam.getExamDate(), exam.getStartTime(), exam.getEndTime(), exam.getStatus()));
                }
            }
        }

        List<DashboardDtos.CalendarEvent> todaySchedule = calendarEvents(
                        today.atStartOfDay(), today.plusDays(1).atStartOfDay(), authorizationHeader).stream()
                .map(this::toCalendarEvent)
                .toList();
        long todayClassCount = todaySchedule.stream()
                .filter(event -> "CLASS".equals(event.eventType()))
                .count();

        TeacherDashboardResponse data = TeacherDashboardResponse.builder()
                .courseCount(courses.size())
                .studentCount(studentIds.size())
                .activeAssignmentCount(activeAssignments)
                .pendingReviewCount(pendingReviews)
                .upcomingExamCount(upcomingExams.size())
                .todayClassCount(todayClassCount)
                .todaySchedule(todaySchedule)
                .pendingGrading(pendingGrading)
                .coursePerformance(performance)
                .build();
        return ApiResponse.<TeacherDashboardResponse>builder()
                .message("Teacher dashboard fetched successfully")
                .data(data)
                .statusCode(200)
                .status(org.springframework.http.HttpStatus.OK)
                .build();
    }

    private List<UUID> enrolledStudents(UUID courseId, String authorizationHeader) {
        ApiResponse<List<UUID>> response = timedDependency("entrollment-service", courseId,
                () -> entrollmentServiceClient.getEnrolledStudentIdsByCourse(courseId, authorizationHeader));
        if (response == null || response.getData() == null) {
            throw new IllegalStateException("Enrollment service returned no student list for course " + courseId);
        }
        return response.getData().stream().filter(java.util.Objects::nonNull).distinct().toList();
    }

    private List<AssignmentDashboardResponse> assignments(UUID courseId, String authorizationHeader) {
        ApiResponse<List<AssignmentDashboardResponse>> response = timedDependency("assignment-service", courseId,
                () -> assignmentDashboardClient.getAssignments(courseId, authorizationHeader));
        return response == null || response.getData() == null ? List.of() : response.getData();
    }

    private List<SubmissionDashboardResponse> submissions(UUID assignmentId, String authorizationHeader) {
        ApiResponse<List<SubmissionDashboardResponse>> response = timedDependency(
                "assignment-service submissions", assignmentId,
                () -> assignmentDashboardClient.getSubmissions(assignmentId, authorizationHeader));
        return response == null || response.getData() == null ? List.of() : response.getData();
    }

    private List<ExamDashboardResponse> exams(UUID courseId, String authorizationHeader) {
        ApiResponse<List<ExamDashboardResponse>> response = timedDependency("exam-service", courseId,
                () -> examDashboardClient.getExams(courseId, authorizationHeader));
        return response == null || response.getData() == null ? List.of() : response.getData();
    }

    private List<CalendarDashboardResponse> calendarEvents(LocalDateTime start, LocalDateTime end,
                                                            String authorizationHeader) {
        ApiResponse<List<CalendarDashboardResponse>> response = timedDependency("calender-service", null,
                () -> calendarDashboardClient.getEvents(start, end, authorizationHeader));
        return response == null || response.getData() == null ? List.of() : response.getData();
    }

    private <T> T timedDependency(String service, UUID resourceId, Supplier<T> call) {
        long startedAt = System.nanoTime();
        try {
            return call.get();
        } catch (RuntimeException exception) {
            log.error("Course service dependency call failed: service={}, resourceId={}, elapsedMs={}",
                    service, resourceId, elapsedMillis(startedAt), exception);
            throw exception;
        } finally {
            long elapsedMs = elapsedMillis(startedAt);
            if (elapsedMs >= 1000) {
                log.warn("Slow course service dependency call: service={}, resourceId={}, elapsedMs={}",
                        service, resourceId, elapsedMs);
            }
        }
    }

    private long elapsedMillis(long startedAt) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private DashboardDtos.CalendarEvent toCalendarEvent(CalendarDashboardResponse event) {
        return new DashboardDtos.CalendarEvent(event.getId(), event.getTitle(), event.getEventType(),
                event.getStartDateTime(), event.getEndDateTime(), event.getCourseId(), event.getLocation());
    }

    @Override
    @Transactional
    public ApiResponse<CourseResponse> createCourse(CourseRequest request, String authorizationHeader) {

        log.info("Creating course with title '{}'", request.getCourseName());

        if (courseRepository.existsByCourseNameAndGradeId(request.getCourseName(), request.getGradeId())) {
            log.warn("Duplicate course title attempted: '{}'", request.getCourseName());
            throw new ResourceAlreadyExistsException("Course", "courseName", request.getCourseName());
        }

        validateGradeExists(request.getGradeId());
        UUID assignedTeacherId = resolveCanonicalTeacherId(request.getAssignedTeacherId(), authorizationHeader);
        teacherCourseAccessGuard.assertTeacherCanAssign(assignedTeacherId);
        validateTeacherExists(assignedTeacherId, authorizationHeader);

        Course course = CourseMapper.toCourseEntity(request);
        course.setAssignedTeacherId(assignedTeacherId);
        if (course.isPublished()) {
            course.setPublishedDate(LocalDate.now());
        } else {
            course.setPublishedDate(null);
        }

        Course savedCourse = courseRepository.save(course);

        CourseResponse courseResponse = toCourseResponses(List.of(savedCourse), authorizationHeader).get(0);
        log.info("Course created successfully with ID [{}]", savedCourse.getId());

        return ApiResponse.<CourseResponse>builder()
                .message("Course created successfully")
                .data(courseResponse)
                .statusCode(HttpStatus.CREATED.value())
                .status(HttpStatus.CREATED)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<CourseResponse> updateCourse(UUID courseId, CourseRequest request, String authorizationHeader) {
        log.info("Updating course with ID [{}]", courseId);

        Course existingCourse = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        UUID assignedTeacherId = resolveCanonicalTeacherId(request.getAssignedTeacherId(), authorizationHeader);

        if (teacherCourseAccessGuard.isTeacherAuthenticated()) {
            teacherCourseAccessGuard.assertTeacherOwnsCourse(existingCourse);
            teacherCourseAccessGuard.assertTeacherCanAssign(assignedTeacherId);
        }

        if (courseRepository.existsByCourseNameAndGradeIdAndIdNot(request.getCourseName(), request.getGradeId(), courseId)) {
            log.warn("Duplicate course title attempted on update: '{}'", request.getCourseName());
            throw new ResourceAlreadyExistsException("Course", "courseName", request.getCourseName());
        }

        validateGradeExists(request.getGradeId());
        validateTeacherExists(assignedTeacherId, authorizationHeader);

        existingCourse.setCourseName(request.getCourseName().trim());
        existingCourse.setCourseCode(trimToNull(request.getCourseCode()));
        existingCourse.setCourseDescription(request.getCourseDescription().trim());
        existingCourse.setCourseThumbnail(request.getCourseThumbnail());
        existingCourse.setGradeId(request.getGradeId());
        existingCourse.setAssignedTeacherId(assignedTeacherId);
        existingCourse.setSubject(request.getSubject() != null ? request.getSubject().trim() : null);
        existingCourse.setLevel(trimToNull(request.getLevel()));
        existingCourse.setSemester(trimToNull(request.getSemester()));
        existingCourse.setAcademicYear(trimToNull(request.getAcademicYear()));
        existingCourse.setMaxCapacity(request.getMaxCapacity());
        existingCourse.setCredits(request.getCredits());
        existingCourse.setSchedule(trimToNull(request.getSchedule()));
        existingCourse.setGradingWeights(request.getGradingWeights() == null ? new java.util.HashMap<>() : new java.util.HashMap<>(request.getGradingWeights()));
        existingCourse.setTeachingAssistantIds(request.getTeachingAssistantIds() == null ? new java.util.HashSet<>() : new java.util.HashSet<>(request.getTeachingAssistantIds()));
        existingCourse.setOnline(request.getIsOnline() != null ? request.getIsOnline() : true);

        existingCourse.setVisible(request.getIsVisible() != null ? request.getIsVisible() : true);

        boolean isPublished = request.getIsPublished() != null ? request.getIsPublished() : false;
        existingCourse.setPublished(isPublished);
        if (isPublished) {
            existingCourse.setStatus(com.mentify.enums.CourseStatus.PUBLISHED);
            if (existingCourse.getPublishedDate() == null) {
                existingCourse.setPublishedDate(LocalDate.now());
            }
        } else {
            existingCourse.setStatus(com.mentify.enums.CourseStatus.DRAFT);
            existingCourse.setPublishedDate(null);
        }

        Course savedCourse = courseRepository.save(existingCourse);

        CourseResponse courseResponse = toCourseResponses(List.of(savedCourse), authorizationHeader).get(0);
        log.info("Course updated successfully with ID [{}]", savedCourse.getId());

        return ApiResponse.<CourseResponse>builder()
                .message("Course updated successfully")
                .data(courseResponse)
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    private void validateTeacherExists(UUID teacherId, String authorizationHeader) {
        if (teacherId == null || !userServiceClient.isTeacherExists(teacherId.toString(), authorizationHeader)) {
            throw new ResourceNotFoundException("Teacher", "id", teacherId);
        }
    }

    private UUID resolveCanonicalTeacherId(UUID teacherId, String authorizationHeader) {
        UUID canonicalId = userServiceClient.getTeacherCanonicalId(teacherId.toString(), authorizationHeader);
        return canonicalId == null ? teacherId : canonicalId;
    }

    private void validateGradeExists(UUID gradeId) {
        if (gradeId == null || !gradeRepository.existsById(gradeId)) {
            throw new ResourceNotFoundException("Grade", "id", gradeId);
        }
    }

    @Override
    @Transactional
    public ApiResponse<CourseResponse> getCourseById(UUID courseId) {
        ApiResponse<CourseResponse> response = getCourseById(courseId, null);
        // This overload is retained for internal callers that have no user token.
        // Do not present the persisted legacy value (which is not maintained) as a live count.
        if (response.getData() != null) {
            response.getData().setNumberOfStudents(null);
        }
        return response;
    }

    @Override
    @Transactional
    public ApiResponse<CourseResponse> getCourseById(UUID courseId, String authorizationHeader) {
        log.info("Fetching course with ID [{}]", courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        if (teacherCourseAccessGuard.isTeacherAuthenticated()) {
            teacherCourseAccessGuard.assertTeacherOwnsCourse(course);
        }

        CourseResponse courseResponse = toCourseResponses(List.of(course), authorizationHeader).get(0);

        return ApiResponse.<CourseResponse>builder()
                .message("Course fetched successfully")
                .data(courseResponse)
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<List<CourseResponse>> getAllCourses() {
        return getAllCourses(null);
    }

    @Override
    @Transactional
    public ApiResponse<List<CourseResponse>> getAllCourses(String authorizationHeader) {
        List<CourseResponse> courses = toCourseResponses(courseRepository.findAll(), authorizationHeader);

        return ApiResponse.<List<CourseResponse>>builder()
                .message("Courses fetched successfully")
                .data(courses)
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<List<CourseResponse>> getCoursesForTeacher(UUID teacherId) {
        return getCoursesForTeacher(teacherId, null);
    }

    @Override
    public ApiResponse<List<CourseResponse>> getCoursesForTeacher(UUID teacherId, String authorizationHeader) {
        List<Course> teacherCourses = courseRepository
                .findAllByAssignedTeacherIdOrderByCreatedAtDesc(teacherId);
        // Pass the caller token through so the summary fields are populated from
        // the same enrollment/assignment/exam/quiz services used by the teacher
        // dashboard. Without it, the mapper left student and assessment counts
        // at zero or null even when the course had data.
        List<CourseResponse> courses = toCourseResponses(teacherCourses, authorizationHeader);

        return ApiResponse.<List<CourseResponse>>builder()
                .message("Teacher courses fetched successfully")
                .data(courses)
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    private List<CourseResponse> toCourseResponses(List<Course> source, String authorizationHeader) {
        if (source.isEmpty()) {
            return List.of();
        }

        Set<UUID> courseIds = source.stream().map(Course::getId).collect(Collectors.toSet());
        java.util.Map<UUID, Long> moduleCounts = moduleRepository == null ? java.util.Map.of()
                : moduleRepository.countByCourseIds(courseIds).stream()
                .collect(Collectors.toMap(CourseCountProjection::getCourseId, CourseCountProjection::getCount));
        java.util.Map<UUID, Long> lessonCounts = lessonRepository == null ? java.util.Map.of()
                : lessonRepository.countByCourseIds(courseIds).stream()
                .collect(Collectors.toMap(CourseCountProjection::getCourseId, CourseCountProjection::getCount));

        return source.stream().map(course -> {
            CourseResponse response = toCourseResponse(course);
            response.setModuleCount(moduleCounts.getOrDefault(course.getId(), 0L));
            response.setLessonCount(lessonCounts.getOrDefault(course.getId(), 0L));
            if (authorizationHeader != null && !authorizationHeader.isBlank() && assignmentDashboardClient != null) {
                response.setAssignmentCount(countAssignments(course.getId(), authorizationHeader));
            }
            if (authorizationHeader != null && !authorizationHeader.isBlank() && entrollmentServiceClient != null) {
                try {
                    response.setNumberOfStudents(enrolledStudents(course.getId(), authorizationHeader).size());
                } catch (RuntimeException exception) {
                    log.warn("Unable to resolve live student count for course [{}]", course.getId(), exception);
                    response.setNumberOfStudents(null);
                }
            }
            if (authorizationHeader != null && !authorizationHeader.isBlank() && examDashboardClient != null) {
                response.setExamCount(countExams(course.getId(), authorizationHeader));
            }
            if (authorizationHeader != null && !authorizationHeader.isBlank() && quizGradebookClient != null) {
                response.setQuizCount(countQuizzes(course.getId(), authorizationHeader));
            }
            return response;
        }).toList();
    }

    private Long countAssignments(UUID courseId, String authorizationHeader) {
        try {
            List<AssignmentDashboardResponse> response = assignments(courseId, authorizationHeader);
            return response == null ? 0L : response.size();
        } catch (RuntimeException exception) {
            log.warn("Unable to resolve assignment count for course [{}]", courseId, exception);
            return null;
        }
    }

    private Long countExams(UUID courseId, String authorizationHeader) {
        try {
            return (long) exams(courseId, authorizationHeader).size();
        } catch (RuntimeException exception) {
            log.warn("Unable to resolve exam count for course [{}]", courseId, exception);
            return null;
        }
    }

    private Long countQuizzes(UUID courseId, String authorizationHeader) {
        try {
            ApiResponse<List<com.mentify.client.dto.QuizGradebookResponse>> response = timedDependency(
                    "assignment-service quizzes", courseId,
                    () -> quizGradebookClient.getQuizzes(courseId, authorizationHeader));
            return response == null || response.getData() == null ? 0L : response.getData().size();
        } catch (RuntimeException exception) {
            log.warn("Unable to resolve quiz count for course [{}]", courseId, exception);
            return null;
        }
    }

    @Override
    @Transactional
    public ApiResponse<List<CourseResponse>> getCoursesByIds(Set<UUID> ids) {
        return getCoursesByIds(ids, null);
    }

    @Override
    @Transactional
    public ApiResponse<List<CourseResponse>> getCoursesByIds(Set<UUID> ids, String authorizationHeader) {
        log.info("Fetching {} courses in bulk", ids.size());

        List<CourseResponse> courses = toCourseResponses(courseRepository.findAllById(ids), authorizationHeader);

        return ApiResponse.<List<CourseResponse>>builder()
                .message("Courses fetched successfully")
                .data(courses)
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<Object> deleteCourse(UUID courseId) {
        log.info("Deleting course with ID [{}]", courseId);

        Course existingCourse = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        courseRepository.delete(existingCourse);

        log.info("Course deleted successfully with ID [{}]", courseId);

        return ApiResponse.builder()
                .message("Course deleted successfully")
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    private CourseResponse toCourseResponse(Course course) {
        Grade grade = gradeRepository.findById(course.getGradeId()).orElse(null);
        return CourseMapper.toCourseResponse(course, grade);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
