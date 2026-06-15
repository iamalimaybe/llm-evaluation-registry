package com.aliniaz.llmeval.evaluationbatch.api.response;

import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunStatus;
import com.aliniaz.llmeval.regression.domain.RegressionComparisonOutcome;

import java.math.BigDecimal;
import java.util.List;

public record EvaluationBatchCaseComparisonResponse(
        Long evaluationCaseId,
        Long baselineRunId,
        Long candidateRunId,
        EvaluationRunStatus baselineStatus,
        EvaluationRunStatus candidateStatus,
        Boolean baselinePassed,
        Boolean candidatePassed,
        BigDecimal baselineScore,
        BigDecimal candidateScore,
        BigDecimal scoreDelta,
        RegressionComparisonOutcome outcome,
        List<String> regressionReasons
) {
}