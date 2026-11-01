
package com.onlineexam.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "exams")
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 120)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExamStatus status = ExamStatus.DRAFT;

    @Column(name = "total_marks", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalMarks = BigDecimal.ZERO;

    @Column(name = "results_published_at")
    private Instant resultsPublishedAt;

    @Column(name = "created_by", nullable = false)
    private Long createdById;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Exam() {
        // Required by JPA
    }

    public Exam(
            String title,
            String description,
            String subject,
            String instructions,
            Integer durationMinutes,
            Instant startsAt,
            Instant endsAt,
            Long createdById
    ) {
        this.title = title;
        this.description = description;
        this.subject = subject;
        this.instructions = instructions;
        this.durationMinutes = durationMinutes;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdById = createdById;
        this.status = ExamStatus.DRAFT;
        this.totalMarks = BigDecimal.ZERO;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(Instant startsAt) {
        this.startsAt = startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(Instant endsAt) {
        this.endsAt = endsAt;
    }

    public ExamStatus getStatus() {
        return status;
    }

    public void setStatus(ExamStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(BigDecimal totalMarks) {
        this.totalMarks = totalMarks;
    }

    public Instant getResultsPublishedAt() {
        return resultsPublishedAt;
    }

    public void setResultsPublishedAt(Instant resultsPublishedAt) {
        this.resultsPublishedAt = resultsPublishedAt;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
