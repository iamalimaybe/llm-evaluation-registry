package com.aliniaz.llmeval.prompt.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePromptVersionRequest(

        @NotBlank(message = "Prompt version is required")
        @Size(max = 50, message = "Prompt version must not exceed 50 characters")
        String version,

        @NotBlank(message = "Prompt text is required")
        String promptText,

        @Size(max = 1000, message = "Change notes must not exceed 1000 characters")
        String changeNotes
) {
}