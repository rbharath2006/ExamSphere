
package com.onlineexam.backend.controller;

import com.onlineexam.backend.dto.CreateExamRequest;
import com.onlineexam.backend.dto.ExamResponse;
import com.onlineexam.backend.service.ExamService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher/exams")
public class TeacherExamController {

    private final ExamService examService;

    public TeacherExamController(ExamService examService) {
        this.examService = examService;
    }

    /**
     * POST /api/teacher/exams
     *
     * Creates a new examination.
     */
    @PostMapping
    public ResponseEntity<ExamResponse> createExam(

            @Valid @RequestBody
            CreateExamRequest request,

            Authentication authentication

    ) {

        ExamResponse response = examService.createExam(
                authentication.getName(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
