
package com.onlineexam.backend.repository;

import com.onlineexam.backend.entity.ExamAttempt;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamAttemptRepository
        extends JpaRepository<ExamAttempt, Long> {

    // Check whether a student already has an attempt.
    boolean existsByExamIdAndStudentId(
            Long examId,
            Long studentId
    );

    // Retrieve a student's attempt for a particular exam.
    Optional<ExamAttempt> findByExamIdAndStudentId(
            Long examId,
            Long studentId
    );

    // Find an attempt only if it belongs to the given student.
    Optional<ExamAttempt> findByIdAndStudentId(
            Long id,
            Long studentId
    );

    // Retrieve all attempts belonging to a student.
    List<ExamAttempt> findByStudentId(Long studentId);

    // Retrieve attempts belonging to an examination.
    List<ExamAttempt> findByExamId(Long examId);
}
