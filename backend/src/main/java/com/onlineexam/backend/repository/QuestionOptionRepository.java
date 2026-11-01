
package com.onlineexam.backend.repository;

import com.onlineexam.backend.entity.QuestionOption;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionOptionRepository
        extends JpaRepository<QuestionOption, Long> {

    // Retrieve options in display order.
    List<QuestionOption> findByQuestionIdOrderByDisplayOrderAsc(
            Long questionId
    );

    // Find an option belonging to a specific question.
    Optional<QuestionOption> findByIdAndQuestionId(
            Long id,
            Long questionId
    );

    // Count options belonging to a question.
    long countByQuestionId(Long questionId);
}
