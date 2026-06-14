package com.aliniaz.llmeval.regression.service;

import com.aliniaz.llmeval.regression.api.response.EvaluationRunComparisonResponse;

public interface RegressionComparisonService {

    EvaluationRunComparisonResponse compareRuns(
            Long workflowId,
            Long baselineRunId,
            Long candidateRunId
    );
}