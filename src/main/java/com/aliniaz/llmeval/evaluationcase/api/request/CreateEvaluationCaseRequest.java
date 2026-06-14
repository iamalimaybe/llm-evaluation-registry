package com.aliniaz.llmeval.evaluationcase.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record CreateEvaluationCaseRequest(

        @NotBlank(message = "Evaluation case name is required")
        @Size(max = 150, message = "Evaluation case name must not exceed 150 characters")
        String name,

        @NotBlank(message = "Task type is required")
        @Size(max = 80, message = "Task type must not exceed 80 characters")
        String taskType,

        @NotEmpty(message = "Input payload is required")
        Map<String, Object> inputPayload,

        Map<String, Object> expectedOutput,

        List<String> requiredFacts,

        List<String> forbiddenClaims,

        Map<String, Object> scoringRules
) {
}