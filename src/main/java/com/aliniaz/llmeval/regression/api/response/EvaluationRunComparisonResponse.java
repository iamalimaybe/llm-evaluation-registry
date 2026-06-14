package com.aliniaz.llmeval.regression.api.response;

import java.math.BigDecimal;
import java.util.List;

public record EvaluationRunComparisonResponse(
        Long workflowId,
        Long baselineRunId,
        Long candidateRunId,
        BigDecimal baselineScore,
        BigDecimal candidateScore,
        BigDecimal scoreDelta,
        Boolean baselinePassed,
        Boolean candidatePassed,
        String outcome,
        List<String> regressionReasons
) {
}