package com.aliniaz.llmeval.evaluationbatch.service;

import com.aliniaz.llmeval.evaluationbatch.api.response.EvaluationBatchComparisonResponse;

public interface EvaluationBatchComparisonService {

    EvaluationBatchComparisonResponse compareEvaluationBatches(
            Long workflowId,
            Long baselineBatchId,
            Long candidateBatchId
    );
}