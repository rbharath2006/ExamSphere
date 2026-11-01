
package com.onlineexam.backend.repository;

import com.onlineexam.backend.entity.UserAccount;
import com.onlineexam.backend.entity.UserRole;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserAccountRepository
        extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    boolean existsByRole(UserRole role);

    boolean existsByEmailIgnoreCase(String email);

    long countByRole(UserRole role);

    long countByEnabledTrue();

    long countByEnabledFalse();

    /**
     * Search and filter Student and Teacher accounts.
     * Admin accounts are excluded.
     */
    @Query("""
    SELECT u
    FROM UserAccount u
    WHERE u.role <> :adminRole
    AND (:selectedRole IS NULL OR u.role = :selectedRole)
    AND (
        LOWER(u.fullName) LIKE CONCAT('%', LOWER(:searchTerm), '%')
        OR LOWER(u.email) LIKE CONCAT('%', LOWER(:searchTerm), '%')
    )
    """)
    Page<UserAccount> searchManagedUsers(
            @Param("selectedRole") UserRole selectedRole,
            @Param("searchTerm") String searchTerm,
            @Param("adminRole") UserRole adminRole,
            Pageable pageable
    );
}
