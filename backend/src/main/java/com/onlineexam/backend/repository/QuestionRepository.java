
package com.onlineexam.backend.repository;

import com.onlineexam.backend.entity.Question;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    // Retrieve questions in their configured order.
    List<Question> findByExamIdOrderByDisplayOrderAsc(Long examId);

    // Find a question belonging to a particular examination.
    Optional<Question> findByIdAndExamId(
            Long id,
            Long examId
    );

    // Count questions belonging to an examination.
    long countByExamId(Long examId);
}
