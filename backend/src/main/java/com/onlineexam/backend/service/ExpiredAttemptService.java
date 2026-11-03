package com.onlineexam.backend.service;

import com.onlineexam.backend.entity.AttemptStatus;
import com.onlineexam.backend.entity.ExamAttempt;
import com.onlineexam.backend.repository.ExamAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ExpiredAttemptService {
    private final ExamAttemptRepository attempts;
    private final ExamAttemptEvaluationService evaluation;

    public ExpiredAttemptService(
            ExamAttemptRepository attempts,
            ExamAttemptEvaluationService evaluation
    ) {
        this.attempts = attempts;
        this.evaluation = evaluation;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AttemptStatus expireIfNeeded(Long attemptId, Long studentId) {
        ExamAttempt attempt = attempts.findByIdAndStudentIdForUpdate(attemptId, studentId)
                .orElseThrow();
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS
                || Instant.now().isBefore(attempt.getExpiresAt())) {
            return null;
        }
        attempt.setStatus(AttemptStatus.AUTO_SUBMITTED);
        attempt.setSubmittedAt(Instant.now());
        attempts.saveAndFlush(attempt);
        evaluation.evaluate(attempt);
        return AttemptStatus.AUTO_SUBMITTED;
    }
}
