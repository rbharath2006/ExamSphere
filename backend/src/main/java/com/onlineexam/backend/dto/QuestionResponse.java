
package com.onlineexam.backend.dto;

import com.onlineexam.backend.entity.Question;
import com.onlineexam.backend.entity.QuestionOption;
import com.onlineexam.backend.entity.QuestionType;

import java.math.BigDecimal;
import java.util.List;

public record QuestionResponse(
        Long id,
        Long examId,
        String questionText,
        QuestionType questionType,
        BigDecimal marks,
        BigDecimal negativeMarks,
        Integer displayOrder,
        List<QuestionOptionResponse> options
) {

    public static QuestionResponse from(
            Question question,
            List<QuestionOption> options
    ) {

        List<QuestionOptionResponse> optionResponses =
                options.stream()
                        .map(QuestionOptionResponse::from)
                        .toList();

        return new QuestionResponse(
                question.getId(),
                question.getExamId(),
                question.getQuestionText(),
                question.getQuestionType(),
                question.getMarks(),
                question.getNegativeMarks(),
                question.getDisplayOrder(),
                optionResponses
        );
    }
}
