
package com.onlineexam.backend.dto;

import com.onlineexam.backend.entity.QuestionOption;

public record QuestionOptionResponse(
        Long id,
        String optionText,
        Integer displayOrder,
        boolean isCorrect
) {

    public static QuestionOptionResponse from(QuestionOption option) {
        return new QuestionOptionResponse(
                option.getId(),
                option.getOptionText(),
                option.getDisplayOrder(),
                option.isCorrect()
        );
    }
}
