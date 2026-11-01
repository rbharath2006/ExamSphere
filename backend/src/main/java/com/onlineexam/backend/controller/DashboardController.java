
package com.onlineexam.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class DashboardController {

    @GetMapping("/admin/dashboard")
    public Map<String, String> adminDashboard() {

        return Map.of(
                "role", "ADMIN",
                "message", "Welcome to the Admin Dashboard"
        );
    }

    @GetMapping("/teacher/dashboard")
    public Map<String, String> teacherDashboard() {

        return Map.of(
                "role", "TEACHER",
                "message", "Welcome to the Teacher Dashboard"
        );
    }

    @GetMapping("/student/dashboard")
    public Map<String, String> studentDashboard() {

        return Map.of(
                "role", "STUDENT",
                "message", "Welcome to the Student Dashboard"
        );
    }
}
