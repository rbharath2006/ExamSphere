
package com.onlineexam.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateQuestionOptionRequest(

        @NotBlank(message = "Option text is required")
        @Size(max = 1000, message = "Option text is too long")
        String optionText,

        boolean isCorrect

) {
}
