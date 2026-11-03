
package com.onlineexam.backend.repository;

import com.onlineexam.backend.entity.Exam;
import com.onlineexam.backend.entity.ExamStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    // Retrieve an exam only when it belongs to the given Teacher.
    Optional<Exam> findByIdAndCreatedById(
            Long id,
            Long createdById
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query(
            "select e from Exam e where e.id = :id and e.createdById = :createdById"
    )
    Optional<Exam> findByIdAndCreatedByIdForUpdate(
            @Param("id") Long id,
            @Param("createdById") Long createdById
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query(
            "select e from Exam e where e.id = :id"
    )
    Optional<Exam> findByIdForUpdate(@Param("id") Long id);

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

    List<Exam> findByStatusOrderByStartsAtAsc(ExamStatus status);

    List<Exam> findByStatusInOrderByStartsAtAsc(List<ExamStatus> statuses);
}
