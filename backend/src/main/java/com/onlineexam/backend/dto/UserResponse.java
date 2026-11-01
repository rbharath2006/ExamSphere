
package com.onlineexam.backend.dto;

import com.onlineexam.backend.entity.UserRole;
import com.onlineexam.backend.entity.UserAccount;

import java.time.Instant;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        UserRole role,
        Instant createdAt
) {

    public static UserResponse from(UserAccount user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}

