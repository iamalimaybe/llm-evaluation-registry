package com.aliniaz.llmeval.evaluationbatch.api.request;

import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public record CreateEvaluationBatchRequest(
        @NotNull
        Long promptVersionId,

        @NotBlank
        @Size(max = 120)
        String modelName,

        @Size(max = 120)
        String modelVersion,

        EvaluationRunProvider provider,

        BigDecimal temperature,

        Map<String, Object> runConfig
) {
}