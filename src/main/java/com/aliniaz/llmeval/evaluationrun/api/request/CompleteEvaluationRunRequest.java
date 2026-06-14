package com.aliniaz.llmeval.evaluationrun.api.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CompleteEvaluationRunRequest(

        @NotBlank(message = "Raw output is required")
        String rawOutput,

        Map<String, Object> parsedOutput,

        @DecimalMin(value = "0.00", message = "Confidence must be at least 0")
        @DecimalMax(value = "100.00", message = "Confidence must not exceed 100")
        BigDecimal confidence,

        @NotNull(message = "Passed flag is required")
        Boolean passed,

        @NotNull(message = "Score is required")
        @DecimalMin(value = "0.00", message = "Score must be at least 0")
        @DecimalMax(value = "100.00", message = "Score must not exceed 100")
        BigDecimal score,

        List<String> failureReasons,

        @Size(max = 2000, message = "Reviewer notes must not exceed 2000 characters")
        String reviewerNotes
) {
}