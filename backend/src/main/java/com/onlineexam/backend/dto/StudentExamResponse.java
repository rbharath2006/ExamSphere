package com.onlineexam.backend.dto;

import com.onlineexam.backend.entity.ExamStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record StudentExamResponse(
        Long id,
        String title,
        String subject,
        String instructions,
        Integer durationMinutes,
        Instant startsAt,
        Instant endsAt,
        ExamStatus status,
        BigDecimal totalMarks,
        boolean upcoming,
        boolean available,
        boolean attempted,
        Long attemptId
) {}
