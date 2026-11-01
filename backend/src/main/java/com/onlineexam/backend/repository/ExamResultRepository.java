
package com.onlineexam.backend.repository;

import com.onlineexam.backend.entity.ExamResult;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExamResultRepository
        extends JpaRepository<ExamResult, Long> {

    // Retrieve the result for a particular attempt.
    Optional<ExamResult> findByAttemptId(Long attemptId);

    // Check whether an attempt has already been evaluated.
    boolean existsByAttemptId(Long attemptId);
}
