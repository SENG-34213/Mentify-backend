package com.mentify.material.security;

import com.mentify.material.client.CourseServiceClient;
import com.mentify.material.client.EnrollmentServiceClient;
import com.mentify.material.client.dto.CourseLookupResponse;
import com.mentify.material.entity.Material;
import com.mentify.material.exception.MaterialValidationException;
import com.mentify.payload.response.ApiResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaterialAuthorizationServiceImpl implements MaterialAuthorizationService {

    private final CurrentMaterialUserService currentUserService;
    private final CourseServiceClient courseServiceClient;
    private final EnrollmentServiceClient enrollmentServiceClient;

    @Override
    public UUID requireUploadCourseAccess(UUID courseId, String authorizationHeader) {
        AuthenticatedMaterialUser user = currentUserService.getCurrentUser();
        if (courseId == null) {
            throw new MaterialValidationException("Course id is required for material upload");
        }

        CourseLookupResponse course = loadCourse(courseId, authorizationHeader);
        if (user.isAdmin()) {
            return course.id();
        }
        if (user.isTeacher() && user.userId().equals(course.assignedTeacherId())) {
            return course.id();
        }
        throw new AccessDeniedException("User is not authorized to upload material for this course");
    }

    @Override
    public void assertCanView(Material material, String authorizationHeader) {
        assertAuthorizedForMaterial(material, authorizationHeader, "view");
    }

    @Override
    public void assertCanDownload(Material material, String authorizationHeader) {
        assertAuthorizedForMaterial(material, authorizationHeader, "download");
    }

    @Override
    public void assertCanManage(Material material, String authorizationHeader) {
        AuthenticatedMaterialUser user = currentUserService.getCurrentUser();
        if (user.isAdmin()) {
            return;
        }
        if (user.isTeacher() && isTeacherAuthorizedForMaterial(user, material, authorizationHeader)) {
            return;
        }
        throw new AccessDeniedException("User is not authorized to manage this material");
    }

    @Override
    public Specification<Material> visibilitySpecification(String authorizationHeader) {
        AuthenticatedMaterialUser user = currentUserService.getCurrentUser();
        if (user.isAdmin()) {
            return null;
        }
        if (user.isTeacher()) {
            List<UUID> courseIds = teacherCourseIds(authorizationHeader);
            return (root, query, cb) -> {
                if (courseIds.isEmpty()) {
                    return cb.equal(root.get("uploadedBy"), user.userId());
                }
                return cb.or(
                        cb.equal(root.get("uploadedBy"), user.userId()),
                        root.get("courseId").in(courseIds)
                );
            };
        }
        if (user.isStudent()) {
            List<UUID> courseIds = studentCourseIds(user.userId(), authorizationHeader);
            return (root, query, cb) -> courseIds.isEmpty()
                    ? cb.disjunction()
                    : root.get("courseId").in(courseIds);
        }
        return (root, query, cb) -> cb.disjunction();
    }

    private void assertAuthorizedForMaterial(Material material, String authorizationHeader, String action) {
        AuthenticatedMaterialUser user = currentUserService.getCurrentUser();
        if (user.isAdmin()) {
            return;
        }
        if (user.isTeacher() && isTeacherAuthorizedForMaterial(user, material, authorizationHeader)) {
            return;
        }
        if (user.isStudent() && isStudentAuthorizedForMaterial(user, material, authorizationHeader)) {
            return;
        }
        throw new AccessDeniedException("User is not authorized to " + action + " this material");
    }

    private boolean isTeacherAuthorizedForMaterial(
            AuthenticatedMaterialUser user,
            Material material,
            String authorizationHeader
    ) {
        if (user.userId().equals(material.getUploadedBy())) {
            return true;
        }
        if (material.getCourseId() == null) {
            return false;
        }
        CourseLookupResponse course = loadCourse(material.getCourseId(), authorizationHeader);
        return user.userId().equals(course.assignedTeacherId());
    }

    private boolean isStudentAuthorizedForMaterial(
            AuthenticatedMaterialUser user,
            Material material,
            String authorizationHeader
    ) {
        if (material.getCourseId() == null) {
            return false;
        }
        return callDownstream(() -> enrollmentServiceClient.isStudentEnrolledInCourse(
                user.userId(),
                material.getCourseId(),
                authorizationHeader
        ));
    }

    private CourseLookupResponse loadCourse(UUID courseId, String authorizationHeader) {
        ApiResponse<CourseLookupResponse> response = callDownstream(
                () -> courseServiceClient.getCourseById(courseId, authorizationHeader)
        );
        if (response == null || response.getData() == null) {
            throw new AccessDeniedException("Course authorization could not be verified");
        }
        return response.getData();
    }

    private List<UUID> teacherCourseIds(String authorizationHeader) {
        ApiResponse<List<CourseLookupResponse>> response = callDownstream(
                () -> courseServiceClient.getMyCourses(authorizationHeader)
        );
        if (response == null || response.getData() == null) {
            return List.of();
        }
        return response.getData().stream()
                .map(CourseLookupResponse::id)
                .toList();
    }

    private List<UUID> studentCourseIds(UUID studentId, String authorizationHeader) {
        ApiResponse<List<UUID>> response = callDownstream(
                () -> enrollmentServiceClient.getActiveCourseIdsForStudent(studentId, authorizationHeader)
        );
        if (response == null || response.getData() == null) {
            return List.of();
        }
        return response.getData();
    }

    private <T> T callDownstream(DownstreamCall<T> call) {
        try {
            return call.execute();
        } catch (FeignException.Forbidden | FeignException.Unauthorized ex) {
            throw new AccessDeniedException("Material authorization was denied by a downstream service", ex);
        } catch (FeignException.NotFound ex) {
            throw new AccessDeniedException("Material resource authorization could not be verified", ex);
        } catch (FeignException ex) {
            throw new AccessDeniedException("Material authorization could not be verified", ex);
        }
    }

    @FunctionalInterface
    private interface DownstreamCall<T> {
        T execute();
    }
}
