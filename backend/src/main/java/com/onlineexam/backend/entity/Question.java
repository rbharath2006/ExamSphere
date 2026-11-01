
package com.onlineexam.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    private QuestionType questionType;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal marks;

    @Column(name = "negative_marks", nullable = false, precision = 8, scale = 2)
    private BigDecimal negativeMarks = BigDecimal.ZERO;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Question() {
        // Required by JPA
    }

    public Question(
            Long examId,
            String questionText,
            QuestionType questionType,
            BigDecimal marks,
            BigDecimal negativeMarks,
            Integer displayOrder
    ) {
        this.examId = examId;
        this.questionText = questionText;
        this.questionType = questionType;
        this.marks = marks;
        this.negativeMarks = negativeMarks == null
                ? BigDecimal.ZERO
                : negativeMarks;
        this.displayOrder = displayOrder;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getExamId() {
        return examId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public BigDecimal getMarks() {
        return marks;
    }

    public void setMarks(BigDecimal marks) {
        this.marks = marks;
    }

    public BigDecimal getNegativeMarks() {
        return negativeMarks;
    }

    public void setNegativeMarks(BigDecimal negativeMarks) {
        this.negativeMarks = negativeMarks;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
