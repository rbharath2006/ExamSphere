package com.onlineexam.backend.dto;

public record AdminDashboardSummaryResponse(
        long totalUsers,
        long totalStudents,
        long totalTeachers,
        long totalAdmins,
        long activeUsers,
        long disabledUsers
) {
}