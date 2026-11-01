
package com.onlineexam.backend.controller;

import com.onlineexam.backend.dto.CreateTeacherRequest;
import com.onlineexam.backend.dto.UserResponse;
import com.onlineexam.backend.entity.UserAccount;
import com.onlineexam.backend.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
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
}
