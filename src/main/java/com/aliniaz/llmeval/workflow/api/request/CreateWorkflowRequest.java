package com.aliniaz.llmeval.workflow.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWorkflowRequest(

        @NotBlank(message = "Workflow name is required")
        @Size(max = 120, message = "Workflow name must not exceed 120 characters")
        String name,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description
) {
}