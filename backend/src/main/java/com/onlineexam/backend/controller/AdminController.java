
package com.onlineexam.backend.controller;

import com.onlineexam.backend.dto.CreateTeacherRequest;
import com.onlineexam.backend.dto.UserResponse;
import com.onlineexam.backend.entity.UserAccount;
import com.onlineexam.backend.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.onlineexam.backend.dto.AdminDashboardSummaryResponse;
import com.onlineexam.backend.service.AdminDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final AdminDashboardService adminDashboardService;

    public AdminController(
            UserService userService,
            AdminDashboardService adminDashboardService
    ) {
        this.userService = userService;
        this.adminDashboardService = adminDashboardService;
    }

    @PostMapping("/teachers")
    public ResponseEntity<UserResponse> createTeacher(
            @Valid @RequestBody CreateTeacherRequest request
    ) {

        UserAccount teacher = userService.createTeacher(
                request.fullName(),
                request.email(),
                request.password()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(UserResponse.from(teacher));
    }
    @GetMapping("/dashboard/summary")
    public ResponseEntity<AdminDashboardSummaryResponse> getDashboardSummary() {

        return ResponseEntity.ok(
                adminDashboardService.getSummary()
        );
    }
}
