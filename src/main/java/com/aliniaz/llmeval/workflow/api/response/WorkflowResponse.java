package com.aliniaz.llmeval.workflow.api.response;

import com.aliniaz.llmeval.workflow.domain.WorkflowStatus;

import java.time.LocalDateTime;

public record WorkflowResponse(
        Long id,
        String name,
        String description,
        WorkflowStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}