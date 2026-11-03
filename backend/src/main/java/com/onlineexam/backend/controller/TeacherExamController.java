
package com.onlineexam.backend.controller;

import com.onlineexam.backend.dto.CreateExamRequest;
import com.onlineexam.backend.dto.ExamResponse;
import com.onlineexam.backend.service.ExamService;
import com.onlineexam.backend.service.ExamResultPublicationService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/exams")
public class TeacherExamController {

    private final ExamService examService;
    private final ExamResultPublicationService resultPublicationService;

    public TeacherExamController(
            ExamService examService,
            ExamResultPublicationService resultPublicationService
    ) {
        this.examService = examService;
        this.resultPublicationService = resultPublicationService;
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

    @GetMapping
    public ResponseEntity<List<ExamResponse>> getExams(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                examService.getExams(authentication.getName())
        );
    }

    @PostMapping("/{examId}/publish")
    public ResponseEntity<ExamResponse> publishExam(
            @PathVariable Long examId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                examService.publishExam(authentication.getName(), examId)
        );
    }

    @PostMapping("/{examId}/close")
    public ResponseEntity<ExamResponse> closeExam(
            @PathVariable Long examId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                examService.closeExam(authentication.getName(), examId)
        );
    }

    @PostMapping("/{examId}/results/publish")
    public ResponseEntity<ExamResponse> publishResults(
            @PathVariable Long examId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                resultPublicationService.publish(authentication.getName(), examId)
        );
    }
}
