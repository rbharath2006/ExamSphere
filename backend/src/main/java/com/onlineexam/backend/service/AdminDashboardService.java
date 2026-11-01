package com.onlineexam.backend.service;

import com.onlineexam.backend.dto.AdminDashboardSummaryResponse;
import com.onlineexam.backend.entity.UserRole;
import com.onlineexam.backend.repository.UserAccountRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {

    private final UserAccountRepository userRepository;

    public AdminDashboardService(
            UserAccountRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardSummaryResponse getSummary() {

        return new AdminDashboardSummaryResponse(
                userRepository.count(),
                userRepository.countByRole(UserRole.STUDENT),
                userRepository.countByRole(UserRole.TEACHER),
                userRepository.countByRole(UserRole.ADMIN),
                userRepository.countByEnabledTrue(),
                userRepository.countByEnabledFalse()
        );
    }
}