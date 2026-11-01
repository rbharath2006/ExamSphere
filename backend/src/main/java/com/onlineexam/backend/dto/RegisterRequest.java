
package com.onlineexam.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 120,
                message = "Full name must contain 2 to 120 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 254, message = "Email is too long")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 12, max = 64,
                message = "Password must contain 12 to 64 characters")
        String password

) {
}
