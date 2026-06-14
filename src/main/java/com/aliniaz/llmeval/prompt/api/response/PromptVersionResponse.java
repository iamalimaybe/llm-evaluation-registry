package com.aliniaz.llmeval.prompt.api.response;

import com.aliniaz.llmeval.prompt.domain.PromptVersionStatus;

import java.time.LocalDateTime;

public record PromptVersionResponse(
        Long id,
        Long workflowId,
        String version,
        String promptText,
        String changeNotes,
        PromptVersionStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}