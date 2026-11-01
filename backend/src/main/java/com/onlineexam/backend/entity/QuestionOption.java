
package com.onlineexam.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "question_options")
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "option_text", nullable = false, columnDefinition = "TEXT")
    private String optionText;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_correct", nullable = false)
    private boolean correct = false;

    protected QuestionOption() {
        // Required by JPA
    }

    public QuestionOption(
            Long questionId,
            String optionText,
            Integer displayOrder,
            boolean correct
    ) {
        this.questionId = questionId;
        this.optionText = optionText;
        this.displayOrder = displayOrder;
        this.correct = correct;
    }

    public Long getId() {
        return id;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public String getOptionText() {
        return optionText;
    }

    public void setOptionText(String optionText) {
        this.optionText = optionText;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }
}
