package com.aliniaz.llmeval.evaluationbatch.api.response;

import com.aliniaz.llmeval.evaluationbatch.domain.EvaluationBatchStatus;
import com.aliniaz.llmeval.regression.domain.RegressionComparisonOutcome;

import java.math.BigDecimal;
import java.util.List;

public record EvaluationBatchComparisonResponse(
        Long workflowId,
        Long baselineBatchId,
        Long candidateBatchId,

        EvaluationBatchStatus baselineStatus,
        EvaluationBatchStatus candidateStatus,

        int baselineTotalRuns,
        int candidateTotalRuns,

        int baselineCompletedRuns,
        int candidateCompletedRuns,

        int baselinePassedRuns,
        int candidatePassedRuns,

        int baselineFailedRuns,
        int candidateFailedRuns,

        int baselineErroredRuns,
        int candidateErroredRuns,

        BigDecimal baselineAverageScore,
        BigDecimal candidateAverageScore,
        BigDecimal scoreDelta,

        RegressionComparisonOutcome outcome,
        List<String> comparisonReasons,
        List<EvaluationBatchCaseComparisonResponse> caseComparisons
) {
}