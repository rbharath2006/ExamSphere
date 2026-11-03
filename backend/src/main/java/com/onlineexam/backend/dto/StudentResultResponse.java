package com.onlineexam.backend.dto;

import com.onlineexam.backend.entity.AttemptStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record StudentResultResponse(
        Long attemptId,
        String title,
        BigDecimal totalMarks,
        BigDecimal obtainedMarks,
        BigDecimal percentage,
        AttemptStatus status,
        Instant submittedAt
) {}
