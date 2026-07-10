package com.mentify.userservice.controller;

import com.mentify.userservice.dto.ApiResponse;
import com.mentify.userservice.dto.UpdateUserStatusRequest;
import com.mentify.userservice.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * Updates a user's active status.
     *
     * @param userId  the ID of the user whose status is to be updated
     * @param request the request body containing the new active status
     * @return HTTP 200 OK on success
     */
    @PatchMapping("/users/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        adminService.updateUserStatus(userId, request.getActive());
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully"));
    }
}
