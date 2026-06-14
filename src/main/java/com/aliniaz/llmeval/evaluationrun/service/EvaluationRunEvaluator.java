package com.aliniaz.llmeval.evaluationrun.service;

import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRun;

public interface EvaluationRunEvaluator {

    EvaluationResult evaluate(EvaluationRun evaluationRun);
}