package com.onlineexam.backend.service;

import com.onlineexam.backend.dto.ExamResponse;
import com.onlineexam.backend.entity.AttemptStatus;
import com.onlineexam.backend.entity.Exam;
import com.onlineexam.backend.entity.ExamStatus;
import com.onlineexam.backend.entity.UserAccount;
import com.onlineexam.backend.entity.UserRole;
import com.onlineexam.backend.repository.ExamAttemptRepository;
import com.onlineexam.backend.repository.ExamRepository;
import com.onlineexam.backend.repository.ExamResultRepository;
import com.onlineexam.backend.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;

@Service
public class ExamResultPublicationService {
    private final UserAccountRepository users;
    private final ExamRepository exams;
    private final ExamAttemptRepository attempts;
    private final ExamResultRepository results;
    private final Clock clock;

    public ExamResultPublicationService(
            UserAccountRepository users,
            ExamRepository exams,
            ExamAttemptRepository attempts,
            ExamResultRepository results,
            Clock clock
    ) {
        this.users = users;
        this.exams = exams;
        this.attempts = attempts;
        this.results = results;
        this.clock = clock;
    }

    @Transactional
    public ExamResponse publish(String teacherEmail, Long examId) {
        UserAccount teacher = users.findByEmailIgnoreCase(teacherEmail.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Teacher account not found"));
        if (teacher.getRole() != UserRole.TEACHER || !teacher.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only enabled Teachers can publish examination results");
        }

        Exam exam = exams.findByIdAndCreatedByIdForUpdate(examId, teacher.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Examination not found"));

        if (exam.getResultsPublishedAt() != null) {
            return ExamResponse.from(exam);
        }

        if (exam.getStatus() != ExamStatus.CLOSED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only closed examinations can publish results");
        }

        for (var attempt : attempts.findByExamIdForUpdate(examId)) {
            if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "All examination attempts must be completed before publishing results");
            }
            if (!results.existsByAttemptId(attempt.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A completed examination attempt has no calculated result");
            }
        }

        exam.setResultsPublishedAt(clock.instant());
        return ExamResponse.from(exams.saveAndFlush(exam));
    }
}
