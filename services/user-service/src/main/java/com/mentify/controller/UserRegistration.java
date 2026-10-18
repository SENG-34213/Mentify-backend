package com.mentify.controller;

import com.mentify.dto.AdminRegisterUserRequest;
import com.mentify.dto.SuperAdminRegisterAdminRequest;
import com.mentify.dto.StudentLookupRequest;
import com.mentify.dto.StudentLookupResponse;
import com.mentify.dto.UpdateUserStatusRequest;
import com.mentify.dto.UserRegistrationResponse;
import com.mentify.dto.StudentDirectoryProfileResponse;
import com.mentify.enums.Role;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.UserRepository;
import com.mentify.service.AdminRegistrationService;
import com.mentify.service.UserRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserRegistration {

    private final UserRegistrationService userRegistrationService;
    private final AdminRegistrationService adminRegistrationService;
    private final UserRepository userRepository;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserRegistrationResponse>> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UserRegistrationResponse response = userRegistrationService.getCurrentUser(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Current user fetched successfully",
                response
        ));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<UserRegistrationResponse>>> getUsersByRole(
            @RequestParam Role role
    ) {
        List<UserRegistrationResponse> users = userRegistrationService.getUsersByRole(role);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Users fetched successfully",
                users
        ));
    }

    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserRegistrationResponse>> registerUser(
            @Valid @RequestBody AdminRegisterUserRequest request
    ) {
        UserRegistrationResponse response = userRegistrationService.registerUser(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        HttpStatus.CREATED.value(),
                        "User registered successfully. Sign-in credentials will be sent by email.",
                        response
                )
        );
    }

    @PostMapping("/admins/register")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserRegistrationResponse>> registerAdmin(
            @Valid @RequestBody SuperAdminRegisterAdminRequest request
    ) {
        UserRegistrationResponse response = adminRegistrationService.registerAdmin(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        HttpStatus.CREATED.value(),
                        "Admin registered successfully. Sign-in credentials will be sent by email.",
                        response
                )
        );
    }

    @PostMapping("/{userId}/resend-invitation")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> resendInvitation(@PathVariable UUID userId) {
        userRegistrationService.resendInvitation(userId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Invitation email with refreshed temporary credentials sent successfully.",
                        null
                )
        );
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        userRegistrationService.updateUserStatus(userId, request.getActive());

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "User status updated successfully",
                        null
                )
        );
    }

    @GetMapping("/teachers/{teacherId}/exists")
    public ResponseEntity<Boolean> teacherExists(@PathVariable String teacherId) {
        boolean exists = userRepository.existsByKeycloakUserIdAndRole(teacherId, Role.TEACHER);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/students/{studentId}/exists")
    public ResponseEntity<Boolean> studentExists(@PathVariable String studentId) {
        boolean exists = userRepository.existsByKeycloakUserIdAndRole(studentId, Role.STUDENT);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/students/lookup")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<StudentDirectoryProfileResponse>>> getStudentsByIds(
            @RequestParam List<UUID> ids
    ) {
        List<StudentDirectoryProfileResponse> students = userRegistrationService.getStudentsByIds(ids);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "Student profiles fetched successfully", students));
    @PostMapping("/students/lookup")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<StudentLookupResponse>>> lookupStudents(
            @Valid @RequestBody StudentLookupRequest request,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication
    ) {
        boolean mayLookupAllStudents = authentication.getAuthorities().stream()
                .anyMatch(authority -> List.of("ROLE_TEACHER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN")
                        .contains(authority.getAuthority()));
        if (!mayLookupAllStudents) {
            UUID currentStudentId = UUID.fromString(jwt.getSubject());
            if (request.getStudentIds().stream().anyMatch(studentId -> !studentId.equals(currentStudentId))) {
                throw new AccessDeniedException("Students can only look up their own profile");
            }
        }
        List<StudentLookupResponse> students = userRegistrationService.lookupStudents(request);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Student summaries fetched successfully",
                students
        ));
    }
}
