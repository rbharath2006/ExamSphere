
package com.onlineexam.backend.controller;

import com.onlineexam.backend.dto.RegisterRequest;
import com.onlineexam.backend.dto.UserResponse;
import com.onlineexam.backend.entity.UserAccount;
import com.onlineexam.backend.service.UserService;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        UserAccount user = userService.registerStudent(
                request.fullName(),
                request.email(),
                request.password()
        );

        UserResponse response = UserResponse.from(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            Authentication authentication
    ) {

        UserAccount user = userService.findByEmail(
                authentication.getName()
        );

        return ResponseEntity.ok(
                UserResponse.from(user)
        );
    }

}
