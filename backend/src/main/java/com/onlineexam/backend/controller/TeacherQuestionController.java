
package com.onlineexam.backend.controller;

import com.onlineexam.backend.dto.CreateQuestionRequest;
import com.onlineexam.backend.dto.QuestionResponse;
import com.onlineexam.backend.service.QuestionService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/exams/{examId}/questions")
public class TeacherQuestionController {

    private final QuestionService questionService;

    public TeacherQuestionController(
            QuestionService questionService
    ) {
        this.questionService = questionService;
    }

    // Create a question.
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

    // List all questions belonging to an examination.
    @GetMapping
    public ResponseEntity<List<QuestionResponse>> getQuestions(

            @PathVariable Long examId,

            Authentication authentication

    ) {

        List<QuestionResponse> response =
                questionService.getQuestions(
                        authentication.getName(),
                        examId
                );

        return ResponseEntity.ok(response);
    }

    // Edit an existing question.
    @PutMapping("/{questionId}")
    public ResponseEntity<QuestionResponse> updateQuestion(

            @PathVariable Long examId,

            @PathVariable Long questionId,

            @Valid @RequestBody
            CreateQuestionRequest request,

            Authentication authentication

    ) {

        QuestionResponse response =
                questionService.updateQuestion(
                        authentication.getName(),
                        examId,
                        questionId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    // Delete an existing question.
    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> deleteQuestion(

            @PathVariable Long examId,

            @PathVariable Long questionId,

            Authentication authentication

    ) {

        questionService.deleteQuestion(
                authentication.getName(),
                examId,
                questionId
        );

        return ResponseEntity.noContent().build();
    }
}
