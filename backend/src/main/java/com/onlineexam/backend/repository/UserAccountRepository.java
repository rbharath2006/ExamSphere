
package com.onlineexam.backend.repository;

import com.onlineexam.backend.entity.UserAccount;
import com.onlineexam.backend.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository
        extends JpaRepository<UserAccount, Long> {


    Optional<UserAccount> findByEmailIgnoreCase(String email);

    boolean existsByRole(UserRole role);

    boolean existsByEmailIgnoreCase(String email);

}
