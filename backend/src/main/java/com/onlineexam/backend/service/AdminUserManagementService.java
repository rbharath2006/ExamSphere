
package com.onlineexam.backend.service;

import com.onlineexam.backend.dto.AdminUserPageResponse;
import com.onlineexam.backend.dto.AdminUserResponse;
import com.onlineexam.backend.entity.UserAccount;
import com.onlineexam.backend.entity.UserRole;
import com.onlineexam.backend.repository.UserAccountRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminUserManagementService {

    private final UserAccountRepository userRepository;

    public AdminUserManagementService(
            UserAccountRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    /**
     * List and search Student and Teacher accounts.
     */
    @Transactional(readOnly = true)
    public AdminUserPageResponse getUsers(
            UserRole role,
            String search,
            Pageable pageable
    ) {

        // Admin accounts cannot be managed through this service.
        if (role == UserRole.ADMIN) {
            throw new IllegalArgumentException(
                    "Admin accounts cannot be listed through this endpoint"
            );
        }
        String normalizedSearch =
                (search == null || search.isBlank())
                        ? ""
                        : search.trim();

        Page<UserAccount> users =
                userRepository.searchManagedUsers(
                        role,
                        normalizedSearch,
                        UserRole.ADMIN,
                        pageable
                );

        return AdminUserPageResponse.from(users);
    }

    /**
     * Enable or disable a Student or Teacher account.
     */
    @Transactional
    public AdminUserResponse updateUserStatus(
            Long userId,
            boolean enabled
    ) {

        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User account not found"
                        )
                );

        // Prevent this endpoint from modifying Admin accounts.
        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException(
                    "Admin accounts cannot be modified through this endpoint"
            );
        }

        user.setEnabled(enabled);

        UserAccount updatedUser = userRepository.save(user);

        return AdminUserResponse.from(updatedUser);
    }
}
