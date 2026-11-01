
package com.onlineexam.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "exam_results")
public class ExamResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempt_id", nullable = false)
    private Long attemptId;

    @Column(name = "total_marks", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalMarks;

    @Column(name = "obtained_marks", nullable = false, precision = 8, scale = 2)
    private BigDecimal obtainedMarks;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    protected ExamResult() {
        // Required by JPA
    }

    public ExamResult(
            Long attemptId,
            BigDecimal totalMarks,
            BigDecimal obtainedMarks,
            BigDecimal percentage
    ) {
        this.attemptId = attemptId;
        this.totalMarks = totalMarks;
        this.obtainedMarks = obtainedMarks;
        this.percentage = percentage;
    }

    @PrePersist
    protected void onCreate() {
        if (evaluatedAt == null) {
            evaluatedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public BigDecimal getTotalMarks() {
        return totalMarks;
    }

    public BigDecimal getObtainedMarks() {
        return obtainedMarks;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }
}
