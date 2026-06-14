package com.aliniaz.llmeval.evaluationrun.domain;

public enum EvaluationRunStatus {
    PENDING,
    RUNNING,
    OUTPUT_CAPTURED,
    PASSED,
    FAILED,
    ERROR
}