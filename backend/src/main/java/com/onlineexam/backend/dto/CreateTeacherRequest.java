
package com.onlineexam.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTeacherRequest(

        @NotBlank(message = "Teacher name is required")
        @Size(min = 2, max = 120)
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email")
        @Size(max = 254)
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 12, max = 64)
        String password

) {}
