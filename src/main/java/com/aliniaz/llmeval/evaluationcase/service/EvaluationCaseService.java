package com.aliniaz.llmeval.evaluationcase.service;

import com.aliniaz.llmeval.evaluationcase.api.request.CreateEvaluationCaseRequest;
import com.aliniaz.llmeval.evaluationcase.api.response.EvaluationCaseResponse;

import java.util.List;

public interface EvaluationCaseService {

    EvaluationCaseResponse createEvaluationCase(Long workflowId, CreateEvaluationCaseRequest request);

    List<EvaluationCaseResponse> getEvaluationCases(Long workflowId);

    EvaluationCaseResponse getEvaluationCase(Long workflowId, Long evaluationCaseId);
}