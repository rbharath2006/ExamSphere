
package com.onlineexam.backend.controller;

import com.onlineexam.backend.dto.CreateQuestionRequest;
import com.onlineexam.backend.dto.QuestionResponse;
import com.onlineexam.backend.service.QuestionService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher/exams/{examId}/questions")
public class TeacherQuestionController {

    private final QuestionService questionService;

    public TeacherQuestionController(
            QuestionService questionService
    ) {
        this.questionService = questionService;
    }

    /**
     * POST /api/teacher/exams/{examId}/questions
     *
     * Adds a question to a Teacher's draft examination.
     */
    @PostMapping
    public ResponseEntity<QuestionResponse> addQuestion(

            @PathVariable Long examId,

            @Valid @RequestBody
            CreateQuestionRequest request,

            Authentication authentication

    ) {

        QuestionResponse response =
                questionService.addQuestion(
                        authentication.getName(),
                        examId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
