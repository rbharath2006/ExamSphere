
package com.onlineexam.backend.dto;

import com.onlineexam.backend.entity.UserAccount;
import org.springframework.data.domain.Page;

import java.util.List;

public record AdminUserPageResponse(
        List<AdminUserResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static AdminUserPageResponse from(Page<UserAccount> users) {

        List<AdminUserResponse> content = users.getContent()
                .stream()
                .map(AdminUserResponse::from)
                .toList();

        return new AdminUserPageResponse(
                content,
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages(),
                users.isFirst(),
                users.isLast()
        );
    }
}
