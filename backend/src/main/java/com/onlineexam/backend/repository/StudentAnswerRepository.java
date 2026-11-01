
package com.onlineexam.backend.repository;

import com.onlineexam.backend.entity.StudentAnswer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentAnswerRepository
        extends JpaRepository<StudentAnswer, Long> {

    // Retrieve all answers for an examination attempt.
    List<StudentAnswer> findByAttemptId(Long attemptId);

    // Retrieve answers in the order they were recorded.
    List<StudentAnswer> findByAttemptIdOrderByAnsweredAtAsc(
            Long attemptId
    );

    // Find the answer to a particular question in an attempt.
    Optional<StudentAnswer> findByAttemptIdAndQuestionId(
            Long attemptId,
            Long questionId
    );

    // Check whether an answer already exists.
    boolean existsByAttemptIdAndQuestionId(
            Long attemptId,
            Long questionId
    );
}
