
package com.onlineexam.backend.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "exam_attempts")
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ExamAttempt() {
        // Required by JPA
    }

    public ExamAttempt(
            Long examId,
            Long studentId,
            Instant expiresAt
    ) {
        this.examId = examId;
        this.studentId = studentId;
        this.expiresAt = expiresAt;
        this.status = AttemptStatus.IN_PROGRESS;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (startedAt == null) {
            startedAt = now;
        }

        if (createdAt == null) {
            createdAt = now;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getExamId() {
        return examId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public void setStatus(AttemptStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
