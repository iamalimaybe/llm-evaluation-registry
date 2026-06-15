package com.aliniaz.llmeval.evaluationrun.service;

import com.aliniaz.llmeval.evaluationrun.api.request.CompleteEvaluationRunRequest;
import com.aliniaz.llmeval.evaluationrun.api.request.CreateEvaluationRunRequest;
import com.aliniaz.llmeval.evaluationrun.api.response.EvaluationRunResponse;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunProvider;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface EvaluationRunService {

    EvaluationRunResponse createEvaluationRun(Long workflowId, CreateEvaluationRunRequest request);

    List<EvaluationRunResponse> getEvaluationRuns(Long workflowId);

    EvaluationRunResponse getEvaluationRun(Long workflowId, Long evaluationRunId);

    EvaluationRunResponse completeEvaluationRun(
            Long workflowId,
            Long evaluationRunId,
            CompleteEvaluationRunRequest request
    );

    EvaluationRunResponse executeEvaluationRun(Long workflowId, Long evaluationRunId);

    EvaluationRunResponse createAndExecuteBatchRun(
            Long workflowId,
            Long promptVersionId,
            Long evaluationCaseId,
            Long batchId,
            String modelName,
            String modelVersion,
            EvaluationRunProvider provider,
            BigDecimal temperature,
            Map<String, Object> runConfig
    );

    List<EvaluationRunResponse> getEvaluationRunsByBatch(Long workflowId, Long batchId);
}