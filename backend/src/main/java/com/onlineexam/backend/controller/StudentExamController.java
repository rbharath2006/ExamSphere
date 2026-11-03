package com.onlineexam.backend.controller;

import com.onlineexam.backend.dto.*;
import com.onlineexam.backend.service.StudentExamService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/student")
public class StudentExamController {
    private final StudentExamService service;
    public StudentExamController(StudentExamService service) { this.service = service; }

    @GetMapping("/exams")
    public List<StudentExamResponse> exams(Authentication auth) {
        return service.listExams(auth.getName());
    }

    @PostMapping("/exams/{examId}/attempts/start")
    public StudentAttemptResponse start(@PathVariable Long examId, Authentication auth) {
        return service.start(auth.getName(), examId);
    }

    @GetMapping("/attempts/{attemptId}")
    public StudentAttemptResponse attempt(@PathVariable Long attemptId, Authentication auth) {
        return service.getAttempt(auth.getName(), attemptId);
    }

    @PutMapping("/attempts/{attemptId}/answers")
    public ResponseEntity<Void> answer(
            @PathVariable Long attemptId,
            @Valid @RequestBody SaveAnswerRequest request,
            Authentication auth
    ) {
        if (!service.saveAnswer(auth.getName(), attemptId, request)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This attempt expired and was auto-submitted");
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public ResponseEntity<StudentResultResponse> submit(
            @PathVariable Long attemptId,
            Authentication auth
    ) {
        StudentResultResponse result = service.submit(auth.getName(), attemptId);
        if (!service.resultsPublished(auth.getName(), attemptId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Exam results have not been published yet");
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/attempts/{attemptId}/result")
    public StudentResultResponse result(@PathVariable Long attemptId, Authentication auth) {
        return service.result(auth.getName(), attemptId);
    }
}
