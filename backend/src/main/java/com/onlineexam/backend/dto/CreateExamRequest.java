
package com.onlineexam.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateExamRequest(

        @NotBlank(message = "Exam title is required")
        @Size(max = 200, message = "Title cannot exceed 200 characters")
        String title,

        @Size(max = 10000, message = "Description is too long")
        String description,

        @NotBlank(message = "Subject is required")
        @Size(max = 120, message = "Subject cannot exceed 120 characters")
        String subject,

        @Size(max = 10000, message = "Instructions are too long")
        String instructions,

        @NotNull(message = "Exam duration is required")
        @Min(value = 1, message = "Duration must be at least 1 minute")
        @Max(value = 600, message = "Duration cannot exceed 600 minutes")
        Integer durationMinutes,

        @NotNull(message = "Exam start time is required")
        Instant startsAt,

        @NotNull(message = "Exam end time is required")
        Instant endsAt

) {
}
