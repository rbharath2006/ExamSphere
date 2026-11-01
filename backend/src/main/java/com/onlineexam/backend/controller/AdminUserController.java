
package com.onlineexam.backend.controller;

import com.onlineexam.backend.dto.AdminUserPageResponse;
import com.onlineexam.backend.dto.AdminUserResponse;
import com.onlineexam.backend.dto.UpdateUserStatusRequest;
import com.onlineexam.backend.entity.UserRole;
import com.onlineexam.backend.service.AdminUserManagementService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserManagementService userManagementService;

    public AdminUserController(
            AdminUserManagementService userManagementService
    ) {
        this.userManagementService = userManagementService;
    }

    /**
     * GET /api/admin/users
     *
     * Lists Students and Teachers.
     * Supports role filtering, searching and pagination.
     */
    @GetMapping
    public ResponseEntity<AdminUserPageResponse> getUsers(

            @RequestParam(required = false)
            UserRole role,

            @RequestParam(required = false)
            String search,

            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable

    ) {

        AdminUserPageResponse response =
                userManagementService.getUsers(
                        role,
                        search,
                        pageable
                );

        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/admin/users/{id}/status
     *
     * Enables or disables a Student or Teacher account.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminUserResponse> updateUserStatus(

            @PathVariable Long id,

            @Valid @RequestBody
            UpdateUserStatusRequest request

    ) {

        AdminUserResponse response =
                userManagementService.updateUserStatus(
                        id,
                        request.enabled()
                );

        return ResponseEntity.ok(response);
    }
}

