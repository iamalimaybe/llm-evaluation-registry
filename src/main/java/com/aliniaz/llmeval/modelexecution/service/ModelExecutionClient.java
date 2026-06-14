package com.aliniaz.llmeval.modelexecution.service;

import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunProvider;

public interface ModelExecutionClient {

    boolean supports(EvaluationRunProvider provider);

    ModelExecutionResponse execute(ModelExecutionRequest request);
}