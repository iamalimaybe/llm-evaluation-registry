package com.aliniaz.llmeval.evaluationrun.api.request;

import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunProvider;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public record CreateEvaluationRunRequest(

        @NotNull(message = "Prompt version id is required")
        Long promptVersionId,

        @NotNull(message = "Evaluation case id is required")
        Long evaluationCaseId,

        @NotBlank(message = "Model name is required")
        @Size(max = 120, message = "Model name must not exceed 120 characters")
        String modelName,

        @Size(max = 120, message = "Model version must not exceed 120 characters")
        String modelVersion,

        EvaluationRunProvider provider,

        @DecimalMin(value = "0.00", message = "Temperature must be at least 0")
        @DecimalMax(value = "2.00", message = "Temperature must not exceed 2")
        BigDecimal temperature,

        Map<String, Object> runConfig
) {
}