package com.onlineexam.backend.service;

import com.onlineexam.backend.entity.*;
import com.onlineexam.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ExamAttemptEvaluationService {
    private final ExamRepository exams;
    private final QuestionRepository questions;
    private final QuestionOptionRepository options;
    private final StudentAnswerRepository answers;
    private final ExamResultRepository results;

    public ExamAttemptEvaluationService(
            ExamRepository exams,
            QuestionRepository questions,
            QuestionOptionRepository options,
            StudentAnswerRepository answers,
            ExamResultRepository results
    ) {
        this.exams = exams;
        this.questions = questions;
        this.options = options;
        this.answers = answers;
        this.results = results;
    }

    @Transactional
    public void evaluate(ExamAttempt attempt) {
        if (results.existsByAttemptId(attempt.getId())) return;

        BigDecimal obtained = BigDecimal.ZERO;
        BigDecimal questionTotal = BigDecimal.ZERO;
        for (Question question : questions.findByExamIdOrderByDisplayOrderAsc(attempt.getExamId())) {
            questionTotal = questionTotal.add(question.getMarks());
            StudentAnswer answer = answers.findByAttemptIdAndQuestionId(
                    attempt.getId(), question.getId()).orElse(null);
            if (answer == null) continue;
            QuestionOption selected = options.findByIdAndQuestionId(
                    answer.getSelectedOptionId(), question.getId()).orElse(null);
            if (selected == null) continue;
            if (selected.isCorrect()) obtained = obtained.add(question.getMarks());
            else obtained = obtained.subtract(question.getNegativeMarks());
        }

        Exam exam = exams.findById(attempt.getExamId()).orElseThrow();
        BigDecimal total = exam.getTotalMarks();
        if (total == null || total.compareTo(questionTotal) != 0) {
            throw new IllegalStateException(
                    "Examination total marks do not match its questions");
        }
        obtained = obtained.max(BigDecimal.ZERO).min(total);
        BigDecimal percentage = total.signum() == 0 ? BigDecimal.ZERO :
                obtained.multiply(BigDecimal.valueOf(100))
                        .divide(total, 2, RoundingMode.HALF_UP);
        results.saveAndFlush(new ExamResult(
                attempt.getId(), total, obtained, percentage));
    }
}
