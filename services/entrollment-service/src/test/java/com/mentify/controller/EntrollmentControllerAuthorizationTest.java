package com.mentify.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntrollmentControllerAuthorizationTest {

    private static final String TEACHER_ADMIN_ROLES = "hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')";

    @Test
    void teacherCanManageEnrollmentCrudEndpoints() throws NoSuchMethodException {
        assertEquals(TEACHER_ADMIN_ROLES, preAuthorizeValue("createEntrollment",
                com.mentify.dto.EntrollmentCreateRequest.class, String.class));
        assertEquals(TEACHER_ADMIN_ROLES, preAuthorizeValue("updateEntrollment", java.util.UUID.class,
                com.mentify.dto.EntrollmentUpdateRequest.class, String.class));
        assertEquals(TEACHER_ADMIN_ROLES, preAuthorizeValue("deleteEntrollment", java.util.UUID.class,
                String.class));
        assertEquals(TEACHER_ADMIN_ROLES, preAuthorizeValue("getActiveEntrollmentsByCourse", java.util.UUID.class,
                String.class));
    }

    @Test
    void requiredEnrollmentRoutesAreMapped() {
        assertTrue(Arrays.stream(EntrollmentController.class.getDeclaredMethods())
                .anyMatch(method -> hasPath(method, GetMapping.class, "/courses/{courseId}")));
        assertTrue(Arrays.stream(EntrollmentController.class.getDeclaredMethods())
                .anyMatch(method -> hasPath(method, PostMapping.class, "")));
        assertTrue(Arrays.stream(EntrollmentController.class.getDeclaredMethods())
                .anyMatch(method -> hasPath(method, PutMapping.class, "/{enrollmentId}")));
        assertTrue(Arrays.stream(EntrollmentController.class.getDeclaredMethods())
                .anyMatch(method -> hasPath(method, DeleteMapping.class, "/{enrollmentId}")));
    }

    private String preAuthorizeValue(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = EntrollmentController.class.getDeclaredMethod(methodName, parameterTypes);
        return method.getAnnotation(PreAuthorize.class).value();
    }

    private boolean hasPath(Method method, Class<?> mappingType, String expectedPath) {
        if (mappingType == GetMapping.class) {
            GetMapping mapping = method.getAnnotation(GetMapping.class);
            return mapping != null && Arrays.asList(mapping.value()).contains(expectedPath);
        }
        if (mappingType == PostMapping.class) {
            PostMapping mapping = method.getAnnotation(PostMapping.class);
            if (mapping == null) {
                return false;
            }
            String[] paths = mapping.value();
            return expectedPath.isEmpty() ? paths.length == 0 : Arrays.asList(paths).contains(expectedPath);
        }
        if (mappingType == PutMapping.class) {
            PutMapping mapping = method.getAnnotation(PutMapping.class);
            return mapping != null && Arrays.asList(mapping.value()).contains(expectedPath);
        }
        DeleteMapping mapping = method.getAnnotation(DeleteMapping.class);
        return mapping != null && Arrays.asList(mapping.value()).contains(expectedPath);
    }
}
