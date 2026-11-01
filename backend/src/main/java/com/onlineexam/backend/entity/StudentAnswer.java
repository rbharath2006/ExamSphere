
package com.onlineexam.backend.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "student_answers")
public class StudentAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempt_id", nullable = false)
    private Long attemptId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "selected_option_id", nullable = false)
    private Long selectedOptionId;

    @Column(name = "answered_at", nullable = false)
    private Instant answeredAt;

    protected StudentAnswer() {
        // Required by JPA
    }

    public StudentAnswer(
            Long attemptId,
            Long questionId,
            Long selectedOptionId
    ) {
        this.attemptId = attemptId;
        this.questionId = questionId;
        this.selectedOptionId = selectedOptionId;
    }

    @PrePersist
    protected void onCreate() {
        if (answeredAt == null) {
            answeredAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public Long getSelectedOptionId() {
        return selectedOptionId;
    }

    public void setSelectedOptionId(Long selectedOptionId) {
        this.selectedOptionId = selectedOptionId;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }
}

