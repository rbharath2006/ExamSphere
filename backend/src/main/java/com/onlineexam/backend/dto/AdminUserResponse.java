
package com.onlineexam.backend.dto;

import com.onlineexam.backend.entity.UserAccount;
import com.onlineexam.backend.entity.UserRole;

import java.time.Instant;

public record AdminUserResponse(
        Long id,
        String fullName,
        String email,
        UserRole role,
        boolean enabled,
        Instant createdAt
) {

    public static AdminUserResponse from(UserAccount user) {
        return new AdminUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
