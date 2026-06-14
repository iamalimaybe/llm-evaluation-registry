package com.aliniaz.llmeval.evaluationrun.service;

import com.aliniaz.llmeval.evaluationrun.api.request.CompleteEvaluationRunRequest;
import com.aliniaz.llmeval.evaluationrun.api.request.CreateEvaluationRunRequest;
import com.aliniaz.llmeval.evaluationrun.api.response.EvaluationRunResponse;

import java.util.List;

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
}