package com.onlineexam.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record StudentQuestionResponse(
        Long id,
        int questionNumber,
        String questionText,
        BigDecimal marks,
        BigDecimal negativeMarks,
        List<StudentOptionResponse> options,
        Long selectedOptionId
) {}
