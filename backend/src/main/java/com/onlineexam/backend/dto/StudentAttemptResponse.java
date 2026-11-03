package com.onlineexam.backend.dto;

import com.onlineexam.backend.entity.AttemptStatus;
import java.time.Instant;
import java.util.List;

public record StudentAttemptResponse(
        Long attemptId,
        Long examId,
        String title,
        Integer durationMinutes,
        Instant startedAt,
        Instant expiresAt,
        AttemptStatus status,
        boolean answersAllowed,
        List<StudentQuestionResponse> questions
) {}
