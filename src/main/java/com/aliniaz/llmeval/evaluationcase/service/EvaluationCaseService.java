package com.aliniaz.llmeval.evaluationcase.service;

import com.aliniaz.llmeval.evaluationcase.api.request.CreateEvaluationCaseRequest;
import com.aliniaz.llmeval.evaluationcase.api.response.EvaluationCaseResponse;
import com.aliniaz.llmeval.evaluationcase.domain.EvaluationCase;

import java.util.List;

public interface EvaluationCaseService {

    EvaluationCaseResponse createEvaluationCase(Long workflowId, CreateEvaluationCaseRequest request);

    List<EvaluationCaseResponse> getEvaluationCases(Long workflowId);

    EvaluationCaseResponse getEvaluationCase(Long workflowId, Long evaluationCaseId);

    EvaluationCase getEvaluationCaseEntity(Long workflowId, Long evaluationCaseId);

    List<EvaluationCase> getEnabledEvaluationCaseEntities(Long workflowId);
}