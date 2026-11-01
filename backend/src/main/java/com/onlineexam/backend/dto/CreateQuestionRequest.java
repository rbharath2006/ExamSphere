
package com.onlineexam.backend.dto;

import com.onlineexam.backend.entity.QuestionType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateQuestionRequest(

        @NotBlank(message = "Question text is required")
        @Size(max = 5000, message = "Question text is too long")
        String questionText,

        @NotNull(message = "Question type is required")
        QuestionType questionType,

        @NotNull(message = "Marks are required")
        @DecimalMin(value = "0.01", message = "Marks must be greater than zero")
        @Digits(integer = 6, fraction = 2, message = "Invalid marks format")
        BigDecimal marks,

        @DecimalMin(value = "0.00", message = "Negative marks cannot be negative")
        @Digits(integer = 6, fraction = 2, message = "Invalid negative marks format")
        BigDecimal negativeMarks,

        @NotEmpty(message = "At least two options are required")
        @Size(min = 2, max = 4, message = "A question must contain between 2 and 4 options")
        List<@Valid CreateQuestionOptionRequest> options

) {
}
