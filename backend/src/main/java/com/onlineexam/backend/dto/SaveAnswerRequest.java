package com.onlineexam.backend.dto;

import jakarta.validation.constraints.NotNull;

public record SaveAnswerRequest(
        @NotNull Long questionId,
        @NotNull Long selectedOptionId
) {}
