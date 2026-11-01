
package com.onlineexam.backend.repository;

import com.onlineexam.backend.entity.Exam;
import com.onlineexam.backend.entity.ExamStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    // Retrieve an exam only when it belongs to the given Teacher.
    Optional<Exam> findByIdAndCreatedById(
            Long id,
            Long createdById
    );

    // Retrieve all exams created by a Teacher.
    Page<Exam> findByCreatedById(
            Long createdById,
            Pageable pageable
    );

    // Retrieve a Teacher's exams filtered by status.
    Page<Exam> findByCreatedByIdAndStatus(
            Long createdById,
            ExamStatus status,
            Pageable pageable
    );

    // Count examinations created by a Teacher.
    long countByCreatedById(Long createdById);
}
