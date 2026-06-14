package com.aliniaz.llmeval.common.api.error;

import java.time.LocalDateTime;

public record ApiErrorResponse(
        String error,
        String message,
        LocalDateTime timestamp
) {
}