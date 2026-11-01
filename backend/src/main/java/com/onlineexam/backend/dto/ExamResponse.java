
package com.onlineexam.backend.dto;

import com.onlineexam.backend.entity.Exam;
import com.onlineexam.backend.entity.ExamStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record ExamResponse(

        Long id,
        String title,
        String description,
        String subject,
        String instructions,
        Integer durationMinutes,
        Instant startsAt,
        Instant endsAt,
        ExamStatus status,
        BigDecimal totalMarks,
        Long createdById,
        Instant createdAt,
        Instant updatedAt

) {

    public static ExamResponse from(Exam exam) {

        return new ExamResponse(
                exam.getId(),
                exam.getTitle(),
                exam.getDescription(),
                exam.getSubject(),
                exam.getInstructions(),
                exam.getDurationMinutes(),
                exam.getStartsAt(),
                exam.getEndsAt(),
                exam.getStatus(),
                exam.getTotalMarks(),
                exam.getCreatedById(),
                exam.getCreatedAt(),
                exam.getUpdatedAt()
        );
    }
}
