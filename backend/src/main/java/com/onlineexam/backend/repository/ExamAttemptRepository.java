
package com.onlineexam.backend.repository;

import com.onlineexam.backend.entity.ExamAttempt;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

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

    @org.springframework.data.jpa.repository.Query(
            "select a.id from ExamAttempt a where a.examId = :examId and a.studentId = :studentId"
    )
    Optional<Long> findIdByExamIdAndStudentId(
            @Param("examId") Long examId,
            @Param("studentId") Long studentId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query(
            "select a from ExamAttempt a where a.examId = :examId and a.studentId = :studentId"
    )
    Optional<ExamAttempt> findByExamIdAndStudentIdForUpdate(
            @Param("examId") Long examId,
            @Param("studentId") Long studentId
    );

    // Find an attempt only if it belongs to the given student.
    Optional<ExamAttempt> findByIdAndStudentId(
            Long id,
            Long studentId
    );

    @org.springframework.data.jpa.repository.Query(
            "select a.examId from ExamAttempt a where a.id = :id and a.studentId = :studentId"
    )
    Optional<Long> findExamIdByIdAndStudentId(
            @Param("id") Long id,
            @Param("studentId") Long studentId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query(
            "select a from ExamAttempt a where a.id = :id and a.studentId = :studentId"
    )
    Optional<ExamAttempt> findByIdAndStudentIdForUpdate(
            @Param("id") Long id,
            @Param("studentId") Long studentId
    );

    // Retrieve all attempts belonging to a student.
    List<ExamAttempt> findByStudentId(Long studentId);

    // Retrieve attempts belonging to an examination.
    List<ExamAttempt> findByExamId(Long examId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query(
            "select a from ExamAttempt a where a.examId = :examId order by a.id"
    )
    List<ExamAttempt> findByExamIdForUpdate(@Param("examId") Long examId);

    // Check whether an examination already has student attempts.
    boolean existsByExamId(Long examId);

    List<ExamAttempt> findByStatusAndExpiresAtBefore(
            com.onlineexam.backend.entity.AttemptStatus status,
            java.time.Instant expiresAt
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query(
            "select a from ExamAttempt a where a.status = :status and a.expiresAt < :expiresAt"
    )
    List<ExamAttempt> findByStatusAndExpiresAtBeforeForUpdate(
            @Param("status") com.onlineexam.backend.entity.AttemptStatus status,
            @Param("expiresAt") java.time.Instant expiresAt
    );
}
