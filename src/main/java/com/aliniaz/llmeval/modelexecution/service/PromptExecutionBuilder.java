package com.aliniaz.llmeval.modelexecution.service;

import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRun;

public interface PromptExecutionBuilder {

    String buildPrompt(EvaluationRun evaluationRun);
}