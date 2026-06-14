package com.aliniaz.llmeval.evaluationrun.service;

import java.math.BigDecimal;
import java.util.List;

public record EvaluationResult(
        boolean passed,
        BigDecimal score,
        List<String> failureReasons
) {
}