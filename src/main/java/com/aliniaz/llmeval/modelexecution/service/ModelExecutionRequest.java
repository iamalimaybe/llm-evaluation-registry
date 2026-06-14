package com.aliniaz.llmeval.modelexecution.service;

import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunProvider;

import java.math.BigDecimal;
import java.util.Map;

public record ModelExecutionRequest(
        EvaluationRunProvider provider,
        String modelName,
        String prompt,
        BigDecimal temperature,
        Map<String, Object> runConfig
) {
}