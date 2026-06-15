package com.aliniaz.llmeval.evaluationbatch.service;

import com.aliniaz.llmeval.evaluationbatch.api.request.CreateEvaluationBatchRequest;
import com.aliniaz.llmeval.evaluationbatch.api.response.EvaluationBatchResponse;

import java.util.List;

public interface EvaluationBatchService {

    EvaluationBatchResponse createEvaluationBatch(Long workflowId, CreateEvaluationBatchRequest request);

    List<EvaluationBatchResponse> getEvaluationBatches(Long workflowId);

    EvaluationBatchResponse getEvaluationBatch(Long workflowId, Long batchId);

    EvaluationBatchResponse cancelEvaluationBatch(Long workflowId, Long batchId);
}